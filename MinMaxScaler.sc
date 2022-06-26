/*
Ted Moore
www.tedmooremusic.com
ted@tedmooremusic.com
June 4, 2020
*/

MinMaxScaler {
	var <>ranges;

	*kr {
		arg input, reset = 0;
		var min_max_buf, sig;

		reset = Trig1.kr(reset,ControlDur.ir);

		min_max_buf = LocalBuf.newFrom(Array.fill(2,{
			arg i;
			input.numChannels.collect({
				i.linlin(0,1,100000,-100000);
			});
		}));

		sig = input.numChannels.collect({
			arg i;
			var val = input[i];
			var min_max = BufRd.kr(2,min_max_buf,i,1,1);

			min_max = Select.kr(reset,[min_max,[100000,-100000]]);

			BufWr.kr([min(val,min_max[0]),max(val,min_max[1])],min_max_buf,i,1);
			val.linlin(min_max[0],min_max[1],0,1,\minmax);
		});
		^Sanitize.kr(sig);
	}

	*newClear {
		arg size;
		^super.new.newClear(size);
	}

	newClear {
		arg size;
		ranges = ControlSpec(inf,-inf).dup(size);
	}

	*fit_transform {
		arg data;
		^super.new.fit_transform(data);
	}

	*fit {
		arg data;
		^super.new.fit(data);
	}

	fit {
		arg data;
		this.initRanges(data[0].size);
		//"ranges size: %".format(ranges.size).postln;
		data.do({
			arg entry;
			this.assimilate(entry);
		});
	}

	assimilate {
		arg entry;
		entry.do({
			arg val, i;
			if(val > ranges[i].maxval,{ranges[i].maxval = val});
			if(val < ranges[i].minval,{ranges[i].minval = val});
		});
	}

	transform {
		arg data;
		data = data.collect({
			arg entry;
			entry.collect({
				arg val, i;
				var return = ranges[i].unmap(val);
				if(return.isNaN,{return = 0});
				return;
			});
		});
		^data;
	}

	fit_transform {
		arg data;
		this.fit(data);
		^this.transform(data);
	}

	inverse_transform {
		arg data;
		data = data.collect({
			arg entry;
			entry.collect({
				arg val, i;
				ranges[i].map(val);
			});
		});
		^data;
	}

	assimilate_transform {
		arg entry;
		var return;
		this.assimilate(entry);
		return = this.transform([entry])[0];
		^return;
	}
}