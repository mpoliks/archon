HDVerb {

	* ar {

		|in,
		decay = 3.5,
		mix = 0.08,
		lpf1 = 2000,
		lpf2 = 6000,
		predelay = 0.025,
		mul = 1,
		add = 0|

		var dry, wet, sig;

		dry = in;
		wet = in;
		wet = DelayN.ar(
			wet,
			0.5,
			predelay.clip(0.0001, 0.5));

		wet = 16.collect{
			var temp;
			temp = CombL.ar(
				wet,
				0.1,
				LFNoise1.kr(
					{
						ExpRand(0.02, 0.04)
				}!2).exprange(0.02, 0.099),
				decay
			);
			temp = LPF.ar(temp, lpf1);
		}.sum * 0.25;

		8.do {
			wet = AllpassL.ar(
				wet,
				0.1,
				LFNoise1.kr(
					{
						ExpRand(0.02,0.04)
				}!2).exprange(0.02, 0.099),
				decay
			);
		};

		wet = LeakDC.ar(wet);
		wet = LPF.ar(wet, lpf2, 0.5);

		sig = dry.blend(wet, mix);
		^sig * mul + add;

	}

}

FreezeVerb {

	* ar {
		|in,
		predelay = 1,
		decay = 10,
		lpf = 4500,
		mix = 0.5,
		amp = 1,
		mul = 1,
		add = 0|
		var dry, wet, temp, sig;

		dry = in;
		temp = in;
		wet = 0;
		temp = DelayN.ar(
			temp,
			0.2,
			predelay);

		16.do {
			temp = AllpassN.ar(
				temp,
				0.05,
				{Rand(0.001, 0.05)}!2,
				decay);
			temp - LPF.ar(temp, lpf);
			wet = wet + temp;
		};

		sig = XFade2.ar(
			dry,
			wet,
			mix * 2 - 1,
			amp);

		^ sig * mul + add;

	}

}
