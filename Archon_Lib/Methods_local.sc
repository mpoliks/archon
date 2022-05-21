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
			oct = (((midi - 21) / 12).asInteger + 1).asString;
			pitch = pitch ++ oct;
		});

		^pitch;

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

