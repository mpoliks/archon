/*

The Behavior Module generates playback machine targets based on performance changes.
It operates based on a system of "events" corresponding to envelopes registered by the Analysis module.
Events are measured against a "target", analysed for %∆ changes, and the results inform a series of weights used to probabilistically inform the next target selection.

*/


Behavior {

	var eventCtr = 0, // counts incoming events
	eventTarget, // target event
	prevEvent = 0, // used to store previous target
	prevTarget = \sc, // previous playback machine

	target = \sc, // current playback machine
	targets, // dictionary of available playback machines
	means, weights, // used to store analysis results to calculate

	addr, // netaddr to send to python

	onsetList, offsetList, centList, rolloffList, flatList, rmsList, pitchList;
	// to keep long running totals of timing, spectral, and amplitude characteristics over time

	*new {
		|netAddr|

        ^super.new.init(netAddr)
	}

	init {

		|netAddr|

		addr = netAddr; // passing Python IP & port

		onsetList = List.new();
		offsetList = List.new();
		centList = List.new();
		rolloffList = List.new();
		flatList = List.new();
		rmsList = List.new();
		pitchList = List.new();

		eventTarget = rrand(1,10); // setting first event target randomly

		targets = [\sc, \so, \ma, \te, \an, \hp, \st, \ht, \cp, \gp];

		means = Dictionary[
			\density -> 1.0,
			\windowsize -> 1.0,
			\mainpitch -> 'unpitched',
			\percpitch -> 0.5,
			\avgcent -> 1000,
			\avgrolloff -> 500,
			\avgflat -> 0.1,
			\avgrms -> 0.2
		];

		weights = Dictionary[
			\sc -> 0,
			\so -> 0,
			\ma -> 0,
			\te -> 0,
			\an -> 0,
			\hp -> 0,
			\st -> 0,
			\ht -> 0,
			\cp -> 0,
			\gp -> 0
		];

	}

	processDensity {
		// simple function to calculate event density from a collection of onsets

		|onsets|

		var density = Array.fill(onsets.size - 1, {
			|i|
			onsets[i + 1] - onsets[i]}
		).mean;

		if (density.notNil == false, {
			"WARN: no density specified".postln;
			density = rrand(1.0, 6.0);
		});

		^ density

	}

	processWindow {
		// simple function to calculate windowsize from a collection of onsets and offsets

		|onsets, offsets|

		var windowsize = Array.fill(onsets.size, {
			|i|
			offsets[i] - onsets[i]}
		).mean;

		^ windowsize

	}

	processMainPitch { // simple function to calculate most common pitch

		|pitches|

		var mainpitch = List.new();
		pitches.do{
			|i|
			mainpitch.add(pitches.occurrencesOf(i));
		};

		mainpitch = pitches[mainpitch.maxIndex];

		^ mainpitch
	}

	targetprocessing { // calculate %∆s and set weights

		var eventOffsets = offsetList[
			(eventCtr - prevEvent)..],
		eventOnsets = onsetList[
			(eventCtr - prevEvent)..],
		eventPitches = pitchList[
			(eventCtr - prevEvent)..],
		eventCents = centList[
			(eventCtr - prevEvent)..],
		eventRolloffs = rolloffList[
			(eventCtr - prevEvent)..],
		eventFlats = flatList[
			(eventCtr - prevEvent)..],
		eventRMS = rmsList[
			(eventCtr - prevEvent)..],

		theseMeans = Dictionary [ // grabbing current means
			\density -> this.processDensity(eventOnsets),
			\windowsize -> this.processWindow(eventOnsets, eventOffsets),
			\mainpitch -> this.processMainPitch(eventPitches),
			\percpitch -> (
				(eventPitches.size - eventPitches.occurrencesOf('unpitched'))
				/ eventPitches.size),
			\avgcent -> eventCents.mean,
			\avgrolloff -> eventRolloffs.mean,
			\avgflat -> eventFlats.mean,
			\avgrms -> eventRMS.mean
		],

		// calculating deviation against previous means
		density_delta = (theseMeans.at(\density) / means.at(\density))
			.linlin(0, 2.0, 100, -100, clip: nil),
		windowsize_delta = (theseMeans.at(\windowsize) / means.at(\windowsize))
			.linlin(0, 2.0, -100, 100, clip: nil),
		percpitch_delta = (theseMeans.at(\percpitch) / (means.at(\percpitch) + 0.1))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgcent_delta = (theseMeans.at(\avgcent) / means.at(\avgcent))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgrolloff_delta = (theseMeans.at(\avgrolloff) / means.at(\avgrolloff))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgflat_delta = (theseMeans.at(\avgflat) / means.at(\avgflat))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgrms_delta = (theseMeans.at(\avgrms) / means.at(\avgrms))
			.linlin(0, 2.0, -100, 100, clip: nil),

		//setting weights by playback machine
		w_sc = ((weights.at(\sc) * 100)
		- (density_delta / 10)
		- (windowsize_delta / 10)
		+ (percpitch_delta / 10)
		+ (avgflat_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		w_so = ((weights.at(\sc) * 100)
		+ (density_delta / 10)
		+ (windowsize_delta / 10)
		+ (percpitch_delta / 10)
		- (avgflat_delta / 10)
		- (avgcent_delta / 10)
		- (avgrolloff_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		w_ma = ((weights.at(\ma) * 100)
		+ (density_delta / 10)
		+ (windowsize_delta / 10)
		+ (avgcent_delta / 10)
		+ (avgrolloff_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		w_te = ((weights.at(\te) * 100)
		+ (density_delta / 5)
		- (windowsize_delta / 5)
		+ (avgcent_delta / 10)
		+ (avgrolloff_delta / 10)
		+ (avgrms_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		w_an = ((weights.at(\an) * 100)
		- (density_delta / 5)
		+ (windowsize_delta / 5)
		+ (avgcent_delta / 10)
		+ (avgrolloff_delta / 10)
		- (avgrms_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		w_hp = ((weights.at(\hp) * 100)
		- (density_delta / 5)
		+ (windowsize_delta / 5)
		+ (avgcent_delta / 10)
		+ (avgrolloff_delta / 10)
		- (avgrms_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		w_st = ((weights.at(\st) * 100)
		- (density_delta / 5)
		+ (windowsize_delta / 5)
		- (percpitch_delta / 10)
		- (avgcent_delta / 5)
		+ (avgrms_delta / 10)
		).linlin(0, 100, 0.0, 0.85, clip: \minmax),

		w_ht = ((weights.at(\ht) * 100)
		- (density_delta / 10)
		+ (windowsize_delta / 10)
		+ (percpitch_delta / 10)
		+ (avgrolloff_delta / 10)
		+ (avgflat_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		w_cp = ((weights.at(\cp) * 100)
		- (density_delta / 5)
		+ (windowsize_delta / 5)
		- (percpitch_delta / 10)
		- (avgcent_delta / 5)
		+ (avgrms_delta / 10)
		).linlin(0, 100, 0.0, 0.85, clip: \minmax),

		w_gp = ((weights.at(\gp) * 100)
		- (density_delta / 5)
		+ (percpitch_delta / 10)
		- (avgrms_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		w_va = (50
		+ (density_delta / 5)
		+ (percpitch_delta / 10)
		- (avgflat_delta / 10)
		- (avgrolloff_delta / 10)
		- (avgrms_delta / 10)
		).linlin(0, 100, 0, 10, clip: \minmax),

		// setting target
		calib = [w_sc, w_so, w_ma, w_te, w_an, w_hp, w_st, w_ht, w_cp, w_gp],

		recalib = Array.fill(
			calib.size, {
				|i|
				calib[i] / calib.sum;
		}),

		thisTarget = targets[recalib.windex],

		// resetting current means and weights
		theseWeights = Dictionary[
			\sc -> recalib[0],
			\so -> recalib[1],
			\ma -> recalib[2],
			\te -> recalib[3],
			\an -> recalib[4],
			\hp -> recalib[5],
			\st -> recalib[6],
			\ht -> recalib[7],
			\cp -> recalib[8],
			\gp -> recalib[9]
		];

		means = theseMeans;
		weights = theseWeights;
		~variance = w_va;

		("weights: " + weights.asString).postln;
		("means: " + means.asString).postln;

		if (~targetOverride == true, {
			// to nudge the system toward a new playback machine and to reset weights

			("WARN: Override Engaged, moving toward" + ~specTarget).postln;
			thisTarget = ~specTarget;

			means = Dictionary[
				\density -> 1.0,
				\windowsize -> 1.0,
				\mainpitch -> 'unpitched',
				\percpitch -> 0.5,
				\avgcent -> 1000,
				\avgrolloff -> 500,
				\avgflat -> 0.1,
				\avgrms -> 0.2
			];

			weights = Dictionary[
				\sc -> 0.1,
				\so -> 0.1,
				\ma -> 0.1,
				\te -> 0.1,
				\an -> 0.1,
				\hp -> 0.1,
				\st -> 0.1,
				\ht -> 0.1,
				\cp -> 0.1,
				\gp -> 0.1
			];

			~targetOverride = false;
		});

		eventTarget = eventCtr + rrand(1, 30);

		^ thisTarget // return target

	}

	runningArgs {
		// used to smooth incoming descriptors from the Analysis module to return as playback machine args

		|dist|

		var eventOffsets = offsetList[
			(eventCtr - dist)..],
		eventOnsets = onsetList[
			(eventCtr - dist)..],
		eventPitches = pitchList[
			(eventCtr - dist)..],
		eventCents = centList[
			(eventCtr - dist)..],
		eventRolloffs = rolloffList[
			(eventCtr - dist)..],
		eventFlats = flatList[
			(eventCtr - dist)..],
		eventRMS = rmsList[
			(eventCtr - dist)..],

		theseMeans = Dictionary [
			\density -> this.processDensity(eventOnsets),
			\windowsize -> this.processWindow(eventOnsets, eventOffsets),
			\mainpitch -> this.processMainPitch(eventPitches),
			\percpitch -> (
				(eventPitches.size - eventPitches.occurrencesOf('unpitched'))
				/ eventPitches.size),
			\avgcent -> eventCents.mean,
			\avgrolloff -> eventRolloffs.mean,
			\avgflat -> eventFlats.mean,
			\avgrms -> eventRMS.mean
		];

		^ theseMeans

	}

	receiver { // osc client

		|args|

		var thisTarget = [target, prevTarget],
		selectTarget = thisTarget[
			[
				((eventCtr - prevEvent) / (eventTarget - prevEvent)),
				1 - ((eventCtr - prevEvent)  / (eventTarget - prevEvent))
			].windex
		],
		returnArgs;

		onsetList.add(args[1]);
		offsetList.add(args[2]);
		centList.add(args[3]);
		rolloffList.add(args[4]);
		flatList.add(args[5]);
		rmsList.add(args[6]);
		pitchList.add(args[7]);

		if (eventCtr > eventTarget, // playback machine target selection
			{
				prevTarget = target;
				prevEvent = eventTarget;
				target = this.targetprocessing();
				("OK: Moving To State:" + target.asString + "!").postln;
		});

		if (eventCtr > 3, // playback machine args generation
			{
				returnArgs = this.runningArgs(3)
			},
			{
				returnArgs = this.runningArgs(1)
			}
		);

		("OK: Event No." ++ eventCtr.asString + "complete, next target: " ++ eventTarget.asString ++".").postln;
		eventCtr = eventCtr + 1;


		^ [selectTarget, returnArgs]


	}

}




	