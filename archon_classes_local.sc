Analysis {

	var
	detect = false,
	c_buff = 0,
	r_buff = 0,
	f_buff = 0,
	p_buff = 0,
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
		this.clearBuffers();
	}


	envDetect {

		|msg|

		var peak = msg[3];
		thresh.postln;
		peak.postln;

		if (detect == false,
			{

				if (peak > (thresh * 1.15), {
					this.onsetFunctions();
				});
			},

			{
				if (peak < thresh, {
					this.offsetFunctions();
				},
			{
				if (peak > lPeak, {
						lPeak = peak;
						thresh = lPeak / 2;
						if (thresh < 0.25, thresh = 0.25);
					});
				this.analysisFunctions(msg);
			});
		});
	}

	onsetFunctions {
		detect = true;
		lPeak = 0.25;
		this.clearBuffers();
		detect.postln;
		pitch = 0;
	}

	offsetFunctions {
		detect = false;
		detect.postln;

		if (p_buff.size > 0, {
			("Pitch: " + p_buff.median).postln;
		});

		("Centroid:" + c_buff.median.asInt).postln;
		("Rolloff:" + r_buff.median.asInt).postln;
		("Flatness:" + f_buff.median).postln;

	}

	analysisFunctions {

		|msg|

		var amp = msg[4],
		freq = msg[5],
		hasFreq = msg[6].asBoolean,
		centroid = msg[7],
		flatness = msg[8],
		rolloff = msg[9],
		this_c_buff = c_buff.add(centroid),
		this_r_buff = r_buff.add(rolloff),
		this_f_buff = f_buff.add(flatness),
		this_p_buff;

		c_buff = this_c_buff;
		r_buff = this_r_buff;
		f_buff = this_f_buff;

		if (hasFreq == true, {
			this_p_buff = p_buff.add(freq.asInteger);
			p_buff = this_p_buff;
		});

	}

	clearBuffers {

		c_buff = Array.newClear(0);
		r_buff = Array.newClear(0);
		f_buff = Array.newClear(0);
		p_buff = Array.newClear(0);

	}


}