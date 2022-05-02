Analysis {

	var
	detect = false,

	sr = 0,
	ctr = 0,
	tick = 0,
	addr,

	thresh = 0.01,
	lPeak = 0.01,

	dict;

	*new {
		|r, netAddr|
        ^super.new.init(r, netAddr)
	}


	init {

		|r, netAddr|

		sr = 1000 / r;

		addr = netAddr;
		addr.postln;

		"OK: Analysis Initialized".postln;

	}


	envDetect {

		|msg|

		var peak = msg[3];
		// thresh.postln;
		// peak.postln;

		if (detect == false,
			{

				if (peak > (thresh * 1.15), {
					detect = true;
					this.onsetFunctions();
				});
			},

			{
				if (((peak < thresh) && (dict.values.size > 0)), {
					detect = false;
					this.offsetFunctions();
				},
			{
				if (peak > lPeak, {
						lPeak = peak;
						thresh = lPeak / 2;
						if (thresh < 0.05, thresh = 0.05);
					});
				this.analysisFunctions(msg);
			});
		});
	}

	onsetFunctions {
		lPeak = 0.25;
		dict = Dictionary.new;
		tick = 0;
		detect.postln;
	}

	offsetFunctions {

		var cent = this.getMetric('cent'),
		rolloff = this.getMetric('rolloff'),
		flat = this.getMetric('flat'),
		rms = this.getMetric('rms'),
		pitch = this.getPitch(),
		str = [ctr, cent, flat, rolloff, rms, pitch].archonJSON;

		str.postln;

		addr.sendMsg("/test", str);
		ctr = ctr + 1;

	}

	analysisFunctions {

		|msg|

		msg.postln;

		dict.putAll(
			Dictionary[("a" ++ tick.asString) ->
				Dictionary[
					\cent -> msg[7],
					\flat -> msg[8],
					\rolloff -> msg[9],
					\rms -> msg[4]
					]
				]
			);

		 if (msg[6].asFloat > 0.0, {
			dict.at(("a" ++ tick.asString)).putAll(Dictionary[\pitch -> msg[5].cpsmidi]);
			});

		dict.at("a" ++ tick.asString).postln;

		tick = tick + 1;

	}


	getPitch {

		var plist = List.new(),
		pitch;

		dict.values.size.do {
			|i|
			if (dict.values[i].at('pitch').notNil == true, {
				plist.add(dict.values[i].at('pitch'));
			})
		};

		plist.postln;

		if (plist.size > (dict.values.size / 2), {
			pitch = plist.median.midipitch;
		},
		{
			pitch = "unpitched";
		});

		^ pitch;

	}

	getMetric {

		|metric|

		var result;

		result = Array.fill(
			dict.values.size,{
				|i| dict.values[i].at(metric)
			}).median;

		^ result;

	}

}
