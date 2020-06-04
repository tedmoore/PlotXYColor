// ============================= basic example

(
// data is in 25 dimensions
~n_dims = 25;
// 1000 data points (each dimension can be -100 to 100)
~data = Array.fill(1000,{Array.rand(~n_dims,-100.0,100.0)});
// normalize data
~normed_data = MinMaxScaler.fit_transform(~data);
)

(
//plot
PlotXYColor(~normed_data,{
	arg i;
	i.postln;
	~data[i].postln;
});
)

// ============================== add headers so that the dropdowns have labels
(
~column_labels = ~n_dims.collect({
	arg i;
	"Ted's Feature %".format(i);
});

PlotXYColor(~normed_data,{
	arg i;
	i.postln;
	~data[i].postln;
},~column_labels);
)

// ============================== add labels to the data points
(
~n_dims = 25;
~data = Array.fill(1000,{Array.rand(~n_dims,-100.0,100.0)});

// add a "column" at the front that is a string with a unique id for each point
~data = ~data.collect({
	arg vector, i;
	vector.addFirst("point-%".format(i));
});

// keep only the data points whose first (numerical) value is greater than zero
~data = ~data.select({
	arg vector;
	vector[1] > 0;
});

// how many points did we end up keeping? (started with 1000)
"kept % points".format(~data.size).postln;

// pull off that first column so we can have the ids as an array
~point_labels = ~data.flop[0];

// get rid of that first "column" from the data (so it is stripped of it's id now)
~data = ~data.collect({
	arg vector;
	vector[1..];
});

// normalize the data
~normed_data = MinMaxScaler.fit_transform(~data);

// plot the data and pass in the array of ids
PlotXYColor(~normed_data,{
	arg id;
	id.postln;
},idArray:~point_labels);
)

// ======================================
(
s.options.device_("Soundflower (2ch)");
//Window.closeAll
~audioFile = "/Users/ted/Desktop/plot test.wav";
s.waitForBoot({

	Task({
		var buf, indices;

		//Buffer.freeAll;
		//s.sync;

		buf = Buffer.readChannel(s,~audioFile,channels:[0]);
		indices = Buffer(s);
		s.sync;

		FluidBufNoveltySlice.process(s,buf,indices:indices,feature:1,threshold:0.55,action:{
			indices.loadToFloatArray(action:{
				arg novelties;
				var n_slices = novelties.size - 1;
				//var n_slices = 2;
				var data = Array.newClear(n_slices);
				var headers, normedData;
				n_slices.do({
					arg i;
					var n_frames = novelties[i+1] - novelties[i];
					var features = Buffer(s);
					FluidBufSpectralShape.process(s,buf,novelties[i],n_frames,features:features,action:{
						var stats = Buffer(s);
						FluidBufStats.process(s,features,stats:stats,numDerivs:0,action:{
							stats.loadToFloatArray(action:{
								arg sts;
								i.postln;

								sts = sts.clump(7);

								/*
								each row is a param of stats analysis
								[0] means
								[1] standard deviations
								[2] skewnesses
								[3] kurtoses
								[4] mins
								[5] medians
								[6] maxes

								each column is a feature of the spec shape
								[0] centroid
								[1] spread
								[2] normalized skewness
								[3] normalized kurtosis
								[4] rolloff
								[5] flatness
								[6] crest
								*/

								data[i] = sts[0]; // just look at the means for now
								s.sync;
								stats.free;
							});
						});
						features.free;
					});
				});

				headers = ["centroid","spread","skewness","kurtosis","rolloff","flatness","crest"];
				//data.postln;
				//data.shape.postln;
				normedData = MinMaxScaler.fit_transform(data);
				defer{
					PlotXYColor(normedData,{
						arg hoverIndex, x, y, xindex, yindex;
						var startFrame = novelties[hoverIndex];
						var dur_sec = (novelties[hoverIndex + 1] - startFrame) / s.sampleRate;
						"index:\t\t%".format(hoverIndex).postln;
						data[hoverIndex].do({
							arg val, hi;
							var extra_tab = "";
							if((hi == 1).or(hi == 6),{extra_tab = "\t"});
							"%:\t%%".format(headers[hi],extra_tab,val).postln;
						});
						"dur:\t\t%".format(dur_sec).postln;
						"point:\t\t% , %".format(x.round(0.01),y.round(0.01)).postln;
						"indices:\t% , %".format(xindex,yindex).postln;
						"".postln;
						{
							var sig = PlayBuf.ar(1,buf,1,0,startPos:startFrame);
							sig = sig * EnvGen.kr(Env([0,1,1,0],[0.03,dur_sec-0.06,0.03]),doneAction:2);
							sig.dup;
						}.play;
					},headers);
				}
			});
		});
	},AppClock).play;
});
)