+ SimpleNumber {

	midipitch {
		var midi, pitch, oct, ref;

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

		midi = this;

		if (midi < 21, {
			pitch = 0;
		},
		{
			pitch = ref[(midi - 21) % 12];
			oct = (((midi - 21) / 12).asInteger + 1).asString;
			pitch = pitch + oct;
		});

		^pitch;

	}

}

