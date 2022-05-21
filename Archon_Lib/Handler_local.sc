Handler {

	var state = "halftime", weights, args;


	*new {

        ^super.new.init()
	}


	init {

		"OK: Handler Initialized".postln;

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

	sciarrinoPlayer {

		|buf|

		Pdef(
			\rhythm, Pkill(
				\instrument, \playback,
				\env, 1,
				\dur, Pseq([
					Pwhite(0.05, 0.2, 4),
					Pwhite(0.01, 0.02, 5),
					Pwhite(0.4, 1.2, 4)], 1),
				\rate, Pseq([1.0]++(2.0!2)++[0.5]++(1.0!2)++[2.0], 1),
				\buf, Pshuf(buf, inf),
				\pan, Pwhite(-1, 1),
				\out, Pseq([
					(0!10),
					(~reverbShortBus!4),
					(~reverbLongBus!2),
					[0],
					(~reverbMidBus!2)], inf),
				{
					buf.do {
						|b|
						Buffer.free(b);
						(b.asString + "freed ").postln;
					}
				}
			)
		).play
	}

	machinePlayer {

		|buf|

		Pdef(
			\rhythm, Pkill(
				\instrument, \playback,
				\env, 1,
				\dur, Pwhite(0.05, 0.06),
				\rate, 0.5,
				\rel, 0.24,
				\buf, Pshuf(buf, inf),
				\pan, Pwhite(-1, 1),
				\out, Pseq([
					(0!7),
					(~reverbShortBus!1)],
					45),
				{
					buf.do {
						|b|
						Buffer.free(b);
						(b.asString + "freed ").postln;
					}
				}
			)
		).play
	}

	techPlayer {

		|buf|

		var temp = rrand(0.1, 0.3);

		Pdef(
			\rhythm, Pkill(
				\instrument, \playback,
				\env, 1,
				\dur, temp,
				\rate, Pseq([
        			Pseries({ rrand(1.0, 1.01) }, 1, { rrand(2.0, 2.01) }),
        			Pseries({ rrand(1.0, 1.01) }, 1, { rrand(0.1, 0.7) }),
					Pseries({ rrand(1.0, 1.01) }, { rrand(2.0, 2.02) })], rrand(1, 4)),
				\atk, Pwhite(0.001, 0.01),
				\rel, Pwhite(0.05, 0.14),
				\buf, Pshuf(buf, inf),
				\pan, Pwhite(-0.2, 0.2),
				\out, Pseq([
					(0!3),
					(~reverbShortBus!1)],
					50),
				{
					buf.do {
						|b|
						Buffer.free(b);
						(b.asString + "freed ").postln;
					}
				}
			)
		).play
	}

	stretchPlayer {

		|buf|

		Pdef(
			\rhythm, Pkill(
				\instrument, \playback,
				\env, 1,
				\dur, Pwhite(0.01, 0.6),
				\rate, 0.25,
				\rel, 1.8,
				\buf, Pshuf(buf, inf),
				\pan, Pwhite(-0.2, 0.2),
				\out, Pseq([
					(0!2),
					(~reverbShortBus!1),
					(~reverbMidBus!2),
				],
					rrand(4, 13)),
				{
					buf.do {
						|b|
						Buffer.free(b);
						(b.asString + "freed ").postln;
					}
				}
			)
		).play
	}

	halftimePlayer {

		|buf|

		Pdef(
			\rhythm, Pkill(
				\instrument, \playback,
				\env, 1,
				\dur, Pwhite(0.01, 0.3),
				\rate, Pseq([
					(0.5!3),
					-0.5], inf),
				\rel, 0.9,
				\buf, Pshuf(buf, inf),
				\pan, Pwhite(-0.9, 0.9),
				\out, Pseq([
					(0!1),
					(~reverbShortBus!3),
					(~reverbMidBus!2),
				],
					rrand(4, 13)),
				{
					buf.do {
						|b|
						Buffer.free(b);
						(b.asString + "freed ").postln;
					}
				}
			)
		).play
	}

	granPlayer {

		|buf|

		Pdef(
			\rhythm, Pkill(
				\instrument, \playgran,
				\env, 1,
				\dur, Pwhite(0.01, 1.8),
				\freq, Pwhite(10, 1000),
				\rate, Pseq([
					(0.5!3),
					-2.0], inf),
				\buf, Pshuf(buf, inf),
				\pan, Pwhite(-0.9, 0.9),
				\out, Pseq([
					(~dryBus!1),
					(~reverbShortBus!3),
					(~reverbMidBus!2),
				],
					rrand(2, 4)),
				{
					buf.do {
						|b|
						Buffer.free(b);
						(b.asString + "freed ").postln;
					}
				}
			)
		).play
	}

	stateMachine {

		|buf|

		switch(state,

			"sciarrino", {
				this.sciarrinoPlayer(buf)
			},

			"machine", {
				this.machinePlayer(buf)
			},

			"tech", {
				this.techPlayer(buf)
			},

			"stretch", {
				this.stretchPlayer(buf)
			},

			"halftime", {
				this.halftimePlayer(buf)
			},

			"granplay", {
				this.granPlayer(buf)
			}
		);
	}


	msg_handler {
		|msg, server|

		var buf = List.new();

		msg.postln;

		msg.size.do {

			|i|

			var b;

			if (i < (msg.size - 1), {
				b = Buffer.read(server, msg[i], action: {
					buf.add(b);
					})
				},
				{
				b = Buffer.read(server, msg[i], action: {
					buf.add(b);
					this.stateMachine(buf);
				});
			});
		};
	}

}
