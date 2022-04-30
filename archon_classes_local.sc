Analysis {

	var
	detect = false,

	sr = 0,
	ctr = 0,
	tick = 0,

	thresh = 0.05,
	lPeak = 0.05,

	dict;

	*new {
		|r|
        ^super.new.init(r)
	}


	init {

		|r|

		sr = 1000 / r;

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
		ctr = ctr + 1;

	}

	analysisFunctions {

		|msg|

		dict.putAll(
			Dictionary[("a" ++ tick.asString) ->
				Dictionary[
					\cent -> msg[6],
					\flat -> msg[7],
					\rolloff -> msg[8],
					\rms -> msg[4]
					]
				]
			);

		 if (msg[5] != 0, {
			dict.values[tick].add(\pitch -> msg[5])
			});

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

		if (plist.size > 0, {
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
