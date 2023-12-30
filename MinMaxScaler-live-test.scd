(
Routine{
	y = {
		arg a = 0.5, b = 0.5;
		var sig = MinMaxScaler.kr([a,b],2);
		sig.poll
	}.play;

	s.sync;

	100.do({
		arg i;
		var e = (i % 2) + 0.5;

		y.set(\a,i * e);
		y.set(\b,i * e * -1);

		0.11.wait;
	});
}.play;
)
