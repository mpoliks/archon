Behavior {

	var eventCtr = 0,
	eventTarget = 10,
	dict,
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
		dict = Dictionary.new();
		//eventTarget = rrand(1,10);

		dict = Dictionary[
			\density -> 1.0,
			\windowsize -> 1.0,
			\mainpitch -> 'unpitched',
			\percpitch -> 0.5,
			\avgcent -> 1000,
			\avgrolloff -> 500,
			\avgflat -> 0.1,
			\avgrms -> 0.2
		];
	}

	processDensity {

		var density = Array.fill(onsetList.size - 1, {
			|i|
			onsetList[i + 1] - onsetList[i]}
		).mean;

		^ density

	}

	processWindow {

		var windowsize = Array.fill(onsetList.size, {
			|i|
			offsetList[i] - onsetList[i]}
		).mean;

		^ windowsize

	}

	processMainPitch {

		var mainpitch = List.new();
		pitchList.do{
			|i|
			mainpitch.add(pitchList.occurrencesOf(i));
		};

		mainpitch = pitchList[mainpitch.maxIndex];

		^ mainpitch
	}

	targetprocessing {

		var thisDict = Dictionary [
			\density -> this.processDensity(),
			\windowsize -> this.processWindow(),
			\mainpitch -> this.processMainPitch(),
			\percpitch -> ((
				pitchList.size - pitchList.occurrencesOf('unpitched'))
			/ pitchList.size),
			\avgcent -> centList.mean,
			\avgrolloff -> rolloffList.mean,
			\avgflat -> flatList.mean,
			\avgrms -> rmsList.mean
		].postln,

		density_delta = (thisDict.at(\density) / dict.at(\density))
			.linlin(0, 2.0, -100, 100, clip: nil),
		windowsize_delta = (thisDict.at(\windowsize) / dict.at(\windowsize))
			.linlin(0, 2.0, -100, 100, clip: nil),
		percpitch_delta = (thisDict.at(\percpitch) / dict.at(\percpitch))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgcent_delta = (thisDict.at(\avgcent) / dict.at(\avgcent))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgrolloff_delta = (thisDict.at(\avgrolloff) / dict.at(\avgrolloff))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgflat_delta = (thisDict.at(\avgflat) / dict.at(\avgflat))
			.linlin(0, 2.0, -100, 100, clip: nil),
		avgrms_delta = (thisDict.at(\avgrms) / dict.at(\avgrms))
			.linlin(0, 2.0, -100, 100, clip: nil);

		/*

		TODO:
		weights should be set by an algorithm that factors in each delta
		density∆ * 150% should mean corresponding weights should increase by some fraction
		let's say density∆ = 1.5, then


		w_sc = (w_sc + (density∆ / 10) - windowsize
		).linlin(0, 100, 0, 100, clip: 'minmax')


		*/

		"density delta = ".postln;
		density_delta.postln;
		"windowsize delta = ".postln;
		windowsize_delta.postln;
		"percpitch delta =".postln;
		percpitch_delta.postln;
		"avgcent delta = ".postln;
		avgcent_delta.postln;
		"avgrolloff delta = ".postln;
		avgrolloff_delta.postln;
		"avgflat delta = ".postln;
		avgflat_delta.postln;
		"avgrms delta = ".postln;
		avgrms_delta.postln;

		dict = thisDict;
		eventTarget = eventCtr + rrand(1, 30);

	}

	receiver {
		|args|

		var x;
		onsetList.add(args[1]);
		offsetList.add(args[2]);
		centList.add(args[3]);
		rolloffList.add(args[4]);
		flatList.add(args[5]);
		rmsList.add(args[6]);
		pitchList.add(args[7]);

		eventCtr = eventCtr + 1;
		eventCtr.postln;

		if (eventCtr >= eventTarget,
			{
				//this.target(this.targetprocessing());
				this.targetprocessing();
			},
			{
			//transition weights to destination
			}
		);
	}
}




	