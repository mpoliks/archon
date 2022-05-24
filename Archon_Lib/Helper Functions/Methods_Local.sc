+ SimpleNumber {

	midipitch {
		var midi = this, pitch, oct,

		ref = [
			"A",
			"A#",
			"B",
			"C",
			"C#",
			"D",
			"D#",
			"E",
			"F",
			"F#",
			"G",
			"G#",
		];

		if (midi < 21, {
			pitch = "unpitched";
		},
		{
			pitch = ref[(midi - 21) % 12];
			oct = (((midi - 21) / 12).asInteger).asString;
			pitch = pitch ++ oct;
		});

		^pitch;

	}

}

+ String {

	pitchcps {

		var pitch = this, midi, freq, oct,

		ref = [
			"A",
			"A#",
			"B",
			"C",
			"C#",
			"D",
			"D#",
			"E",
			"F",
			"F#",
			"G",
			"G#",
		];

		if (pitch == "unpitched",
			{
				freq = 0;
			},
			{
				oct = pitch[(pitch.size - 1)..];
				oct.postln;
				pitch = pitch.replace(oct);
				ref.size.do {
					|i|
					if (pitch == ref[i],
						{
							midi = (i + 21) + (12 * oct.asInteger);
							freq = midi.midicps;
					});
				};
		});

		^ freq;

	}

}

+ Array {

	archonJSON{

		var id = this[0],
		cent = this[1],
		flat = this[2],
		rolloff = this[3],
		rms = this[4],
		pitch = this[5],

		str = ("{" ++
			'"' ++ id.asString ++ '"' ++
			':{' ++
			'"cent":' ++
			'"' ++ cent.asString ++ '"' ++
			',' ++
			'"flat":' ++
			'"' ++ flat.asString ++ '"' ++
			',' ++
			'"rolloff":' ++
			'"' ++ rolloff.asString ++ '"' ++
			',' ++
			'"rms":' ++
			'"' ++ rms.asString ++ '"' ++
			',' ++
			'"pitch":' ++
			'"' ++ pitch.asString ++ '"' ++
			'}}').asString;

		^ str;
	}

}