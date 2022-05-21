Analysis {

	var
	detect = false,

	sr = 0,
	ctr = 0,
	tick = 0,
	addr,

	onsetTime,
	offsetTime,

	globals = Dictionary[
		\degree_of_difference -> 0,
		\number_of_samples -> 25,
		\database -> 0.5
	],

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

		"OK: Analysis Initialized".postln;

	}

	envDetect {

		|msg|

		var peak = msg[3];

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
		"OK: Onset Detected".postln;
		onsetTime = Date.getDate.rawSeconds.asString;

	}

	offsetFunctions {

		var cent = this.getMetric('cent'),
		rolloff = this.getMetric('rolloff'),
		flat = this.getMetric('flat'),
		rms = this.getMetric('rms'),
		pitch = this.getPitch(),
		str = [ctr, cent, flat, rolloff, rms, pitch].archonJSON;

		offsetTime = Date.getDate.rawSeconds.asString;
		"OK: Offset Detected".postln;

		str.postln;

		onsetTime = onsetTime[
			(onsetTime.find(".") - 5)..(onsetTime.find(".") + 2)
		].asFloat;
		offsetTime = offsetTime[
			(offsetTime.find(".") - 5)..(offsetTime.find(".") + 2)
		].asFloat;

		NetAddr.localAddr.sendMsg("/behavior",
		onsetTime, offsetTime, cent, rolloff, flat, rms, pitch);

		addr.sendMsg("/test", str);
		ctr = ctr + 1;

	}

	analysisFunctions {

		|msg|

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