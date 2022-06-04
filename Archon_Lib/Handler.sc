/*

The Handler is a sequencer that takes several inputs:
1. it takes buffer information from the Python query,
2. it takes playback machine information (state) from the Behavior module,
3. it takes live feature extraction information (args) from the Analysis module, smoothed by the Behavior module.

*/


Handler {

	var state = \sc, // playback machine
	args; // live features

	*new {

        ^super.new.init()
	}

	init {

		"OK: Handler Initialized".postln;

		args = Dictionary[
			\windowsize -> 0.4, // duration of average event
			\percpitch -> 0.0, // % of an event that is pitched
			\avgflat -> 0.09, // spectral flatness
			\density -> 1.5, // average duration between events
			\avgcent -> 4906.16, // spectral centroid
			\avgrms -> 0.10, // rms amplitude
			\mainpitch -> 'unpitched', // main pitch of event
			\avgrolloff -> 4300 // spectral rolloff
		];


	}

	settings { // function for setting playback machine and playback args

		|msg|

		state = msg[0];
		args = msg[1];

	}

	sciarrinoPlayer { // short flurries of events

		|buf|

		var dur = args.at(\windowsize).linlin(0, 5, 1, 5, clip: \minmax),
		dense = args.at(\density).linlin(0, 5, 1, 5, clip: \minmax),
		bright = args.at(\avgrolloff).linlin(0, 12000, 0, 5, clip: \minmax),
		width = args.at(\avgrms).linlin(0.0, 1.0, 0.3, 1.0, clip: \minmax),
		resonant = args.at(\percpitch).linlin(0.0, 1.0, 1, 4, clip: \minmax),
		noise = args.at(\avgflat).linlin(0.0, 0.1, 1, 3, clip:\minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 0.0, 0.7, clip: \minmax);


		Pkill(

			\instrument, \playback,

			\env, 1,

			\dur, Pseq([
				Pwhite(0.05, 0.2 * dur, (dur + 2).asInteger),
				Pwhite(0.01, 0.02 * dur, (dur + 2).asInteger),
				Pwhite(0.4, 1.2, (dur + 2).asInteger)
			], 1),

			\atk, Pwhite(width / 20, width / 10),

			\rel, Pwhite(dur / 10, dur / 5),

			\rate, Pseq(
				[1.0]++
				(2.0!(dense.asInteger))++
				(0.5!(noise.asInteger))++
				(1.0!(dense.asInteger * 2))++
				(2.0!((dense/2).asInteger)),
				1),

			\buf, Pshuf(buf, inf),

			\pan, Pwhite(-1 * width, width),

			\amp, Pwhite(0.1, 0.2),

			\out, Pseq([
				(~drybus!(bright.asInteger)),
				(~reverbShortBus!4),
				(~reverbLongBus!(resonant.asInteger)),
				(~drybus!1),
				(~reverbMidBus!(resonant.asInteger))], inf),

			{
				Routine {
					10.wait;
					buf.do {
						|b|
						b.free;
					};
				}
			}

		).play
	}

	machinePlayer { // semi-periodic assemblages of events

		|buf|

		var dur = args.at(\density).linlin(0.0, 5.0, 0.2, 1.0, clip: \minmax),
		variance = args.at(\windowsize).linlin(0.0, 3.0, 0.1, 0.3, clip: \minmax),
		bright = args.at(\avgrolloff).linlin(0, 12000, 0, 4, clip: \minmax),
		width = args.at(\avgrms).linlin(0.0, 1.0, 0.0, 1.0, clip: \minmax),
		resonant = args.at(\percpitch).linlin(0.0, 1.0, 7, 2, clip: \minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 0.0, 0.7, clip: \minmax);

		Pkill(

			\instrument, \playback,

			\env, 1,

			\dur, Pwrand(
				[(dur / 32), (dur / 8), (dur / 16), (dur /64), (dur / 32)],
				[16, 1, 1, 2, 1].normalizeSum,
				inf),

			\rate, Pwrand(
				[0.5, 1, 2.0, -1],
				[2, 6, 2, 6].normalizeSum,
				inf),

			\buf, Pshuf(buf, inf, inf),

			\pan, Pwhite(-1, 1, inf),

			//\amp, Pbrown(0.2, 0.1, 0.125, inf),

			\out, Pseq([
				(~reverbShortBus!((bright + 1).asInteger)),
				(~dryBus!(3))
				], (100 * dur).asInteger),

			{
				Routine {
					10.wait;
					buf.do {
						|b|
						b.free;
					};
				}
			}

		).play
	}

	techPlayer { // periodic sequences corresponding to input density

		|buf|

		var temp = args.at(\density).linlin(0.0, 5.0, 0.0, 0.2, clip: \minmax),
		variance = args.at(\windowsize).linlin(0.0, 3.0, 0.0, 0.3, clip: \minmax),
		bright = args.at(\avgrolloff).linlin(0, 12000, 0.1, 0.01, clip: \minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 1.0, 4.0, clip: \minmax),
		noise = args.at(\avgflat).linlin(0.0, 0.1, 1, 3, clip:\minmax);

		Pkill(

			\instrument, \playback,
			\env, 1,

			\dur, temp,

			\rate, Pseq([
				Pseries(
					{ rrand(1.0, 1 + variance) },
					1,
					{ rrand(2.0, 2 + variance) }),
				Pseries(
					{ rrand(1.0, 1 + variance) },
					1,
					{ rrand(0.1, 0.7) }),
				Pseries(
					{ rrand(1.0, 1 + variance) },
					{ rrand(2.0, 2 + 1 + variance) })
			], rrand(
				1, (velocity.asInteger))
			),

			\buf, Pshuf(buf, inf),

			\pan, Pwhite(-1, 1),

			\amp, Pwhite(0.1, 0.2),

			\out, Pseq([
				(~dryBus!3),
				(~reverbShortBus!(noise.asInteger))
			], (20).asInteger),

			{
				Routine {
					10.wait;
					buf.do {
						|b|
						b.free;
					};
				}
			}

		).play
	}

	stretchPlayer { // slowed down audio corresponding to spectral rolloff

		|buf|

		var rates = [0.125, 0.25, 0.5],
		bright = args.at(\avgrolloff).linlin(500, 12000, 0, 2, clip: \minmax),
		rate = rates[bright.asInteger],
		freq = args.at(\mainpitch)
			.asString
			.pitchcps
			.linlin(0, 10000, 0, 10, clip: \mixmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 0.0, 0.7, clip: \minmax);


		Pkill(

			\instrument, \playback,

			\env, 1,

			\dur, Pwhite(0.01, 0.6),

			\rate, rate,

			\rel, 0.5 * (1 / rate),

			\buf, Pshuf(buf, inf),

			\pan, Pwhite(-0.2, 0.2),

			\amp, Pwhite(velocity / 2, velocity),

			\out, Pseq([
				(~dryBus!2),
				(~reverbShortBus!1),
				(~reverbMidBus!(freq.asInteger)),
			], rrand(4, 13)),

			{
				Routine {
					10.wait;
					buf.do {
						|b|
						b.free;
					};
				}
			}

		).play
	}

	halftimePlayer { // stretched audio highly reactive to pitched information

		|buf|

		var freq = args.at(\mainpitch)
			.asString
			.pitchcps
			.linlin(0, 10000, 2, 0.01, clip: \mixmax),
		resonant = args.at(\percpitch).linlin(0.0, 1.0, 7, 2, clip: \minmax),
		bright = args.at(\avgrolloff).linlin(0, 12000, 1, 5, clip: \minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 1.0, 3.0, clip: \minmax);

		Pkill(

			\instrument, \playback,

			\env, 1,

			\dur, Pwhite(0.01, 0.1),

			\rate, Pseq([
				(0.5!3),
				(-0.5!(bright.asInteger))
			], inf),

			\rel, 0.9 / (velocity),

			\buf, Pshuf(buf, inf),

			\pan, Pwhite(-1 * (freq / 2), freq / 2),

			\amp, Pwhite(bright / 10 , bright / 7),

			\out, Pseq([
				(~drybus!(resonant.asInteger)),
				(~reverbShortBus!(resonant.asInteger)),
				(~reverbMidBus!2),
			], rrand(4, (velocity * 20).asInteger)),

			{
				Routine {
					10.wait;
					buf.do {
						|b|
						b.free;
					};
				}
			}

		).play
	}

	granPlayer { // granular synthesis engine corresponding to input frequency

		|buf|

		var freq = args.at(\mainpitch)
			.asString
			.pitchcps
			.linlin(0, 10000, 0, 10000, clip: \mixmax),
		resonant = args.at(\percpitch).linlin(0.0, 1.0, 1, 4, clip: \minmax),
		variance = args.at(\windowsize).linlin(0.0, 3.0, 1.0, 10.0, clip: \minmax),
		dur = args.at(\windowsize).linlin(0, 3, 1, 5, clip: \minmax),
		bright = args.at(\avgrolloff).linlin(0, 4000, 1, 10, clip: \minmax),
		brill = args.at(\avgcent).linlin(0, 3000, 1, 10, clip: \minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 0.0, 0.7, clip: \minmax);

		if (freq == 0, {
			freq = rrand (3, 30);
		});

		Pkill(
			\instrument, \playgran,

			\env, 1,

			\dur, Pwhite(0.01, resonant * 3),

			\atk, Pwhite(1, bright * 2),

			\rel, Pwhite(2 + (velocity * 3), 4 + (velocity * 3 * bright)),

			\freq, Pwhite(freq, freq + freq / 10),

			\rate, Pseq([
				(0.5!(bright.asInteger)),
				(1.0!(resonant.asInteger)),
				-2.0!((freq / 1000).asInteger)
			], inf),

			\cutoff, Pwhite(bright * 1000, bright * 4000),

			\buf, Pshuf(buf, inf),

			\pan, Pwhite(-1 * resonant, resonant),

			\amp, Pwhite(0.3, 0.4),

			\out, Pseq([
				(~reverbShortBus!bright.asInteger),
				(~reverbMidBus!brill.asInteger),
				(~reverbLongBus!(variance.asInteger)),
			], rrand(1, dur)),

			{
				Routine {
					10.wait;
					buf.do {
						|b|
						b.free;
					};
				}
			}

		).play
	}

	stateMachine { // function that relays handler buffer into playback machines

		|buf|

		//args.postln;

		switch(state,

			\sc, {
				this.sciarrinoPlayer(buf)
			},

			\ma, {
				this.machinePlayer(buf)
			},

			\te, {
				this.techPlayer(buf)
			},

			\st, {
				this.stretchPlayer(buf)
			},

			\ht, {
				this.halftimePlayer(buf)
			},

			\gp, {
				this.granPlayer(buf)
			}
		);
	}

	msg_handler { // osc client

		|msg, server|

		var buf = List.new();

		msg.postln;

		msg.size.do {

			|i|

			var b;

			if (i < (msg.size - 1), {
				b = Buffer.readChannel(server, msg[i], 0, -1, [0], action: {
					buf.add(b);
					})
				},
				{
				b = Buffer.readChannel(server, msg[i], 0, -1, [0], action: {
					buf.add(b);
					this.stateMachine(buf);
				});
			});
		};
	}

}
