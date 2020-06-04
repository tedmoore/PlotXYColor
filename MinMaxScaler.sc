/*
Ted Moore
www.tedmooremusic.com
ted@tedmooremusic.com
June 4, 2020
*/

MinMaxScaler {
	var <>ranges;

	initRanges {
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