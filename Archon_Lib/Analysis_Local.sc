/*

The Analysis module interprets incoming audio data and returns extracted features.
It has two outputs:
1. The Python query, which is then routed to the Handler.
2. The Behavior module, which sets Handler behaviors.

This module organizes all information by "events," which are discrete envelopes.

*/


Analysis {

	var detect = false,

	sr = 0, // sample rate
	ctr = 0, // used to increment through events
	tick = 0, // used to increment through event dictionary
	addr, // netaddr to send to python

	onsetTime, // used for Behavior module density and window calculations
	offsetTime, // used for Behavior module density and window calculations

	globals, // TODO: to change Python database settings

	thresh = 0.01, // amplitude setting for envelope detection
	lPeak = 0.01, // local peak for envelope calculation

	dict; // running dictionary of events

	*new {

		|r, netAddr, detection_threshold|

        ^super.new.init(r, netAddr, detection_threshold)

	}

	init {

		|r, netAddr, detection_threshold|

		sr = 1000 / r; // setting sample rate

		addr = netAddr; // passing Python IP & port

		globals = Dictionary[ // TODO: to change Python database settings
			\degree_of_difference -> 0,
			\number_of_samples -> 25,
			\database -> 0.5
		];

		thresh = detection_threshold;
		lPeak = detection_threshold;

		"OK: Analysis Initialized".postln;

	}


	onsetFunctions { // for when a new peak is detected

		lPeak = 0.25; // update current local peak to avoid jitter
		dict = Dictionary.new; // reset descriptor dictionary
		tick = 0; // reset dictionary index
		onsetTime = Date.getDate.rawSeconds.asString; // grab the current time

		"OK: Onset Detected".postln;

	}

	getPitch { // simple function for pitch extraction

		var plist = List.new(),
		pitch;

		dict.values.size.do { // grab all the pitch elements from the dict
			|i|
			if (dict.values[i].at('pitch').notNil == true, {
				plist.add(dict.values[i].at('pitch'));
			})
		};

		if (plist.size > (dict.values.size / 2), {
			// if there's a preponderance of pitched material, return pitch
			pitch = plist.median.midipitch;
		},
		{
			// if not, return unpitched
			pitch = "unpitched";
		});

		^ pitch;

	}

	getMetric { // simple function for grabing nonpitched audio descriptors

		|metric|

		var result;

		result = Array.fill( // parse dictionary for the metric and grab the median
			dict.values.size,{
				|i| dict.values[i].at(metric)
			}).median;

		^ result;

	}

	offsetFunctions { // for when a new offset is detected

		var cent = this.getMetric('cent'),
		rolloff = this.getMetric('rolloff'),
		flat = this.getMetric('flat'),
		rms = this.getMetric('rms'),
		pitch = this.getPitch(),
		str = [ctr, cent, flat, rolloff, rms, pitch].archonJSON;
		// collect median values and pitch and format it for OSC

		offsetTime = Date.getDate.rawSeconds.asString; // grab current time

		onsetTime = onsetTime[
			(onsetTime.find(".") - 5)..(onsetTime.find(".") + 2)
		].asFloat;
		offsetTime = offsetTime[
			(offsetTime.find(".") - 5)..(offsetTime.find(".") + 2)
		].asFloat;
		// format timings so they are not too big to send via OSC

		NetAddr.localAddr.sendMsg("/behavior",
			onsetTime,
			offsetTime,
			cent,
			rolloff,
			flat,
			rms,
			pitch); // send everything to the behavior module

		"OK: Analysis = " + str.postln;

		addr.sendMsg("/query", str); // send to Python
		ctr = ctr + 1; //increment event counter

		"OK: Offset Detected".postln;

	}

	analysisFunctions { // assemble descriptors into dictionary

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
			}); //assemble separate index for pitches

		tick = tick + 1; // increment dictionary index

	}

	envDetect {

		|msg|

		var peak = msg[3]; // grabbing peak amplitude data

		if (detect == false,
		// if nothing has been detected, wait for a new peak

			{

				if (peak > (thresh * 1.15), {
					detect = true;
					this.onsetFunctions();
				});
			},

		//if it has been detected, start pumping descriptors in to the dict
			{
				if (((peak < thresh) && (dict.values.size > 0)), {
					detect = false;
					this.offsetFunctions();
				},

				{
					if (peak > lPeak, {
						lPeak = peak;
						thresh = lPeak / 2;
						// if (thresh < 0.05, thresh = 0.05);
						if (thresh < 0.15, thresh = 0.15);
					});
					this.analysisFunctions(msg);
				});
			});
	}

}