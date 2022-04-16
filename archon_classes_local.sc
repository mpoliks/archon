Analysis {

	var
	detect = false,

	c_buff = 0,
	r_buff = 0,
	f_buff = 0,
	p_buff = 0,
	v_buff = 0,

	a_buff = 0,

	sr = 0,

	thresh = 0.02,
	lPeak = 0.02;

	*new {
		|r|
		r.postln;
        ^super.new.init(r)
	}


	init {

		|r|

		sr = 1000 / r;

		"Analysis Initialized".postln;
		this.clearBuffers();
		a_buff = Array.newClear;

	}


	envDetect {

		|msg|

		var peak = msg[3];
		// thresh.postln;
		// peak.postln;

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
	}

	offsetFunctions {

		var this_p_buff,
		this_c_buff = c_buff.median.asInteger,
		this_r_buff = r_buff.median.asInteger,
		this_v_buff = v_buff.median.asInteger,
		this_f_buff = f_buff.median,
		dur = (v_buff.size * sr),
		this_a_buff;


		detect = false;
		detect.postln;

		if (p_buff.size > 0, {
			this_p_buff = p_buff.median.asInteger;
			("Pitch: " + this_p_buff).postln;
		},
		{
		this_p_buff = 0;
		});

		("Centroid: " + this_c_buff).postln;
		("Rolloff: " + this_r_buff).postln;
		("Flatness: " + this_f_buff).postln;
		("Volume: " + this_v_buff).postln;
		("Duration: " + dur + "ms").postln;

		this_a_buff = a_buff.add([
			this_p_buff,
			this_c_buff,
			this_r_buff,
			this_f_buff,
			this_v_buff,
			dur
		]);
		a_buff = this_a_buff;

		("Record to date:" + a_buff).postln;

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
		this_v_buff = v_buff.add(amp),

		this_p_buff;

		c_buff = this_c_buff;
		r_buff = this_r_buff;
		f_buff = this_f_buff;
		v_buff = this_v_buff;

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
		v_buff = Array.newClear(0);

	}


}
