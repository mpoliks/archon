Analysis {

	var
	detect = false,
	counter = 0,
	thresh = 0.02,
	lPeak = 0.02,
	pitch = 0;

	*new {
		|bar|
        ^super.new.init(bar)
	}


	init {
		|bar|
		"Analysis Initialized".postln;
	}


	envDetect {

		|msg|

		var peak = msg[3];
		thresh.postln;
		peak.postln;

		if (detect == false,
			{

				if (peak > (thresh * 1.15), {
					this.onsetFunctions.();
				});
			},

			{
				if (peak < thresh, {
					this.offsetFunctions.();
				},
			{
				if (peak > lPeak, {
						lPeak = peak;
						thresh = lPeak / 2;
						if (thresh < 0.25, thresh = 0.25);
					});
				this.analysisFunctions.(msg);
			});
		});
	}

	onsetFunctions {
		detect = true;
		lPeak = 0.25;
		detect.postln;
		pitch = 0;
	}

	offsetFunctions {
		detect = false;
		detect.postln;

		if (pitch != 0, {
			("Pitch: " + pitch).postln;
		});
	}

	analysisFunctions {

		|msg|

		var amp = msg[4],
		freq = msg[5],
		hasFreq = msg[6].asBoolean,
		centroid = msg[7],
		flat = msg[8],
		rolloff = msg[9];

		counter += 1;

		if (hasFreq == true, {
			pitch = freq.asInteger;
		});

	}

}