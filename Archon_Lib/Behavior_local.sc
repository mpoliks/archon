Behavior {

	var eventCtr = 0,
	eventTarget = 10,
	prevEvent = 0,
	prevTarget = \sc,
	target = \sc,
	targets,
	means, weights, args,
	onsetList, offsetList, centList,
	rolloffList, flatList, rmsList,
	pitchList;


	*new {

        ^super.new.init()
	}


	init {

		onsetList = List.new();
		offsetList = List.new();
		centList = List.new();
		rolloffList = List.new();
		flatList = List.new();
		rmsList = List.new();
		pitchList = List.new();
		//eventTarget = rrand(1,10);

		targets = [\sc, \ma, \te, \st, \ht, \hp];

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
			\ma -> 0,
			\te -> 0,
			\st -> 0,
			\ht -> 0,
			\gp -> 0
		];

		args = Dictionary[
			\intensity -> 0,
			\brightness -> 0,
			\speed -> 0,
			\periodicity -> 0,
			\frequency -> 0
		];

	}

	processDensity {

		|onsets|

		var density = Array.fill(onsets.size - 1, {
			|i|
			onsets[i + 1] - onsets[i]}
		).mean;

		^ density

	}

	processWindow {

		|onsets, offsets|

		var windowsize = Array.fill(onsets.size, {
			|i|
			offsets[i] - onsets[i]}
		).mean;

		^ windowsize

	}

	processMainPitch {

		|pitches|

		var mainpitch = List.new();
		pitches.do{
			|i|
			mainpitch.add(pitches.occurrencesOf(i));
		};

		mainpitch = pitches[mainpitch.maxIndex];

		^ mainpitch
	}

	targetprocessing {

		// SETTING WEIGHTS

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
		],

		density_delta = (theseMeans.at(\density) / means.at(\density))
			.linlin(0, 2.0, 100, -100, clip: nil),
		windowsize_delta = (theseMeans.at(\windowsize) / means.at(\windowsize))
			.linlin(0, 2.0, -100, 100, clip: nil),
		percpitch_delta = (theseMeans.at(\percpitch) / means.at(\percpitch))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgcent_delta = (theseMeans.at(\avgcent) / means.at(\avgcent))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgrolloff_delta = (theseMeans.at(\avgrolloff) / means.at(\avgrolloff))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgflat_delta = (theseMeans.at(\avgflat) / means.at(\avgflat))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgrms_delta = (theseMeans.at(\avgrms) / means.at(\avgrms))
			.linlin(0, 2.0, -100, 100, clip: nil),

		w_sc = ((weights.at(\sc) * 100)
		- (density_delta / 10)
		- (windowsize_delta / 10)
		+ (percpitch_delta / 10)
		+ (avgflat_delta / 10)
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

		w_st = ((weights.at(\st) * 100)
		- (density_delta / 5)
		+ (windowsize_delta / 5)
		- (percpitch_delta / 10)
		- (avgcent_delta / 10)
		- (avgrms_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		w_ht = ((weights.at(\ht) * 100)
		- (density_delta / 10)
		+ (windowsize_delta / 10)
		+ (percpitch_delta / 10)
		+ (avgrolloff_delta / 10)
		+ (avgflat_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		w_gp = ((weights.at(\gp) * 100)
		- (density_delta / 10)
		+ (percpitch_delta / 5)
		- (avgcent_delta / 10)
		- (avgflat_delta / 10)
		- (avgrms_delta / 10)
		).linlin(0, 100, 0.0, 1.0, clip: \minmax),

		calib = [w_sc, w_ma, w_te, w_st, w_ht, w_gp],

		recalib = Array.fill(
			calib.size, {
				|i|
				calib[i] / calib.sum;
		}),

		thisTarget = targets[recalib.windex],

		theseWeights = Dictionary[
			\sc -> recalib[0],
			\ma -> recalib[1],
			\te -> recalib[2],
			\st -> recalib[3],
			\ht -> recalib[4],
			\gp -> recalib[5]
		];

		means = theseMeans;
		weights = theseWeights;

		eventTarget = eventCtr + rrand(1, 30);

		^ thisTarget

	}

	runningArgs {

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


	receiver {

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

		if (eventCtr > eventTarget,
			{
				prevTarget = target;
				prevEvent = eventTarget;
				target = this.targetprocessing();
		});

		if (eventCtr > 3,
			{
				returnArgs = runningArgs(3)
			},
			{
				runningArgs(1)
			}
		);

		eventCtr = eventCtr + 1;
		eventCtr.postln;


		^ [thisTarget, runningArgs(1)]


	}

}




	