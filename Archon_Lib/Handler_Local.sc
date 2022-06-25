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
		cent = args.at(\avgcent).linlin(0, 10000, 200, 16000, clip: \minmax),
		width = args.at(\avgrms).linlin(0.0, 1.0, 0.3, 1.0, clip: \minmax),
		resonant = args.at(\percpitch).linlin(0.0, 1.0, 1, 4, clip: \minmax),
		noise = args.at(\avgflat).linlin(0.0, 0.1, 1, 3, clip:\minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 0.0, 0.7, clip: \minmax),
		atk = 0.1;

		Pkill(

			\instrument, \playback,

			\env, 1,

			\dur, Pseq([
				Pwhite(0.05, 0.2 * dur, (dur + 2).asInteger),
				Pwhite(0.01, 0.02 * dur, (dur + 2).asInteger),
				Pwhite(0.4, 1.2, (dur + 2).asInteger)
			], 2),

			\atk, atk,

			\rel, ~sampleSize - atk,

			\rate, Pseq(
				[1.0]++
				(2.0!(dense.asInteger))++
				(0.5!(noise.asInteger))++
				(1.0!(dense.asInteger * 2))++
				(2.0!((dense/2).asInteger)),
				2),

			\buf, Pshuf(buf, inf),

			\cutoff, cent,

			\pan, Pwhite(-1 * width, width),

			\amp, Pwhite(0.02, 0.1),

			\out, Pseq([
				(~dryBus[rrand(0, 4) % ~numPairs]!(bright.asInteger)),
				(~reverbShortBus[rrand(0, 4) % ~numPairs]!4),
				(~reverbLongBus[rrand(0, 4) % ~numPairs]!(resonant.asInteger)),
				(~dryBus[1 % ~numPairs]!1),
				(~reverbMidBus[0 % ~numPairs]!(resonant.asInteger))], inf),

			{
				{
					buf.do {
						|b|
						b.free;
					}
				}.defer(20);
			}

		).play
	}

	sonorousPlayer { // flurries of events with reverb

		|buf|

		var dur = args.at(\windowsize).linlin(0, 5, 1, 5, clip: \minmax),
		dense = args.at(\density).linlin(0, 5, 1, 5, clip: \minmax),
		bright = args.at(\avgrolloff).linlin(0, 12000, 0, 5, clip: \minmax),
		cent = args.at(\avgcent).linlin(0, 10000, 200, 16000, clip: \minmax),
		width = args.at(\avgrms).linlin(0.0, 1.0, 0.3, 1.0, clip: \minmax),
		resonant = args.at(\percpitch).linlin(0.0, 1.0, 1, 4, clip: \minmax),
		noise = args.at(\avgflat).linlin(0.0, 0.1, 1, 3, clip:\minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 0.0, 0.7, clip: \minmax),
		reps = rrand(dense, dense * 7),
		granreps = rrand(0, 1),
		choose = rrand(0, 2),
		freq = args.at(\mainpitch).asString.pitchcps,
		atk = rrand(0.01, 0.4);

		Pkill(

			\instrument, \playback,

			\env, 1,

			\dur, Pseq([
				Pwhite(0.05, 0.2 * dur, 5),
				Pwhite(0.01, 0.02 * dur, 6),
				Pwhite(0.4, 2.2, (dur + 2).asInteger)
			], reps),

			\atk, atk,

			\rel, ~sampleSize - atk,

			\rate, Pseq(
				[1.0]++
				(0.5!(noise.asInteger))++
				(-1.0!(dense.asInteger * 2))++
				(1.0!((dense/2).asInteger)),
				inf),

			\buf, Pshuf(buf, inf),

			\cutoff, Env(
				[cent, cent * 2, cent / 2],
				[1.0, 6.0],
				\sin),

			\amp, Env(
				[0.01, 0.1, 0.01],
				[1.0, 6.0],
				\sin),

			\pan, Pwhite(-1 * width, width),

			\out, Pseq([
				(~reverbShortBus[0 % ~numPairs]!2),
				(~reverbMidBus[4 % ~numPairs]!3)], 6),

			{
				{
					buf.do {
						|b|
						b.free;
					}
				}.defer(160);
			}

		).play;

		if ((choose == 0), {

			if ((freq < 30), {
				freq = rrand(6, 60)
			});

			Pkill(

				\instrument, \playgran,

				\dur, dense,

				\env, 1,

				\buf, Pshuf(buf, inf),

				\atk, Pwhite(1.0, 10.0),

				\rel, Pwhite(1.0, 10.0),

				\freq, freq,

				\rate, Pwrand([-1.0, 1.0], [1, 1], reps),

				\cutoff, Env(
					[cent, cent * 5, cent / 2],
					[1, 10],
					\sin),

				\pan, Pwhite(-1 * width, width),

				\amp, Pwhite(0.001, 0.02),

				\out, Pseq([
					(~reverbMidBus[rrand(0, 4) % ~numPairs]),
					(~reverbLongBus[rrand(0, 4) % ~numPairs]),
				], inf),

				{
				}

			).play;
		});

	}

	machinePlayer { // semi-periodic assemblages of events

		|buf|

		var dur = args.at(\density).linlin(0.0, 5.0, 0.2, 1.0, clip: \minmax),
		variance = args.at(\windowsize).linlin(0.0, 3.0, 0.1, 0.3, clip: \minmax),
		bright = args.at(\avgrolloff).linlin(1000, 12000, 1, 4, clip: \minmax),
		cent = args.at(\avgcent).linlin(0, 10000, 200, 16000, clip: \minmax),
		width = args.at(\avgrms).linlin(0.0, 1.0, 0.0, 1.0, clip: \minmax),
		resonant = args.at(\percpitch).linlin(0.0, 1.0, 7, 2, clip: \minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 0.0, 0.7, clip: \minmax),
		cutoff = rrand(200 * velocity, 800 * velocity),
		reps = rrand(10, bright * 50),
		atk = 0.1;

		Pkill(

			\instrument, \playback,

			\env, 1,

			\dur, Pwrand(
				[(dur / 16), (dur / 8), (dur / 16), (dur /32), (dur / 2)],
				[16, 1, 1, 2, 1].normalizeSum,
				inf),

			\rate, Pwrand(
				[0.5, 1, 2.0, -1],
				[2, 6, 1, 6].normalizeSum,
				reps * 2),

			\buf, Pshuf(buf, inf),

			\atk, atk,

			\rel, ~sampleSize - atk,

			\pan, Pwhite(
				rrand(-1, 1),
				(rrand(-1, 1) + 0.3)
					.linlin(-1.0, 1.0, -1.0, 1.0, clip: \minmax),
				inf),

			\cutoff, Env(
				[cent, cent * 2, cent / 2],
				[rrand(2, 10), rrand(2, 40)],
				\sin),

			\amp, Env(
				[0.01, 0.1, 0.01],
				[rrand(2, 10), rrand(2, 40)],
				\sin),

			\out, Pseq([
				(~reverbShortBus[rrand(0, 4) % ~numPairs]!((bright + 1).asInteger)),
				(~dryBus[rrand(0, 4) % ~numPairs]!(3))
				], inf),

			{
				{
					buf.do {
						|b|
						b.free;
					}
				}.defer(20);
			}

		).play
	}

	techPlayer { // periodic sequences corresponding to input density

		|buf|

		var temp = args.at(\density).linlin(0.0, 5.0, 0.1, 3.0, clip: \minmax),
		variance = args.at(\windowsize).linlin(0.0, 3.0, 0.0, 0.3, clip: \minmax),
		cent = args.at(\avgcent).linlin(0, 10000, 16000, 200, clip: \minmax),
		bright = args.at(\avgrolloff).linlin(0, 12000, 0.1, 0.01, clip: \minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 1.0, 4.0, clip: \minmax),
		noise = args.at(\avgflat).linlin(0.0, 0.1, 1, 3, clip:\minmax),
		cutoff = rrand(200 * velocity, 800 * velocity),
		reps = rrand(10, 35).asInteger,
		atk = rrand(0.01, variance + 0.1);

		Pkill(

			\instrument, \playback,

			\env, 1,

			\dur, rrand(temp / 2, temp),

			\rate, Pwrand(
				[0.5, 1, 2.0, -1],
				[2, 6, 1, 6].normalizeSum,
				reps * 2),

			\atk, atk,

			\rel, ~sampleSize - atk,

			\buf, Pshuf(buf, inf),

			\pan, Pwhite(
				rrand(-1, 1),
				(rrand(-1, 1) + 0.2)
					.linlin(-1.0, 1.0, -1.0, 1.0, clip: \minmax),
				inf),

			\amp, Env(
				[0, 0.1, 0],
				[(reps / 2).asInteger, (reps / 2).asInteger],
				\sin),

			\cutoff, Env(
				[cutoff, cutoff * 6, cutoff / 2],
				[(reps / 2).asInteger, (reps / 2).asInteger],
				\sin),

			\out, Pseq([
				(~dryBus[0 % ~numPairs]!3),
				(~reverbShortBus[rrand(0, 4) % ~numPairs]!(noise.asInteger))
			], inf),

			{
				{
					buf.do {
						|b|
						b.free;
					}
				}.defer(20);
			}

		).play
	}

	angelPlayer { // high-pitched, warbling, long envelope

		|buf|

		var temp = args.at(\density).linlin(0.0, 5.0, 0.1, 3.0, clip: \minmax),
		variance = args.at(\windowsize).linlin(0.0, 3.0, 0.0, 0.3, clip: \minmax),
		cent = args.at(\avgcent).linlin(0, 10000, 16000, 200, clip: \minmax),
		bright = args.at(\avgrolloff).linlin(0, 12000, 200, 10000, clip: \minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 1.0, 4.0, clip: \minmax),
		noise = args.at(\avgflat).linlin(0.0, 0.1, 1, 3, clip:\minmax),
		cutoff = rrand(100 * velocity, 400 * velocity),
		rate = (rrand(1, 2) * 2),
		reps = rrand(16, 80).asInteger,
		atk = (0.5 * (~sampleSize / rate));

		Pkill(

			\instrument, \playback,

			\env, 1,

			\dur, Pwhite(0.001, 0.1, inf),

			\rate, Pwrand(
				[rate, (-1 * rate)],
				[2, 2].normalizeSum,
				reps),

			\atk, atk,

			\rel, (~sampleSize - (atk + 0.1)),

			\buf, Pshuf(buf, inf),

			\pan, Pwhite(
				rrand(-1, 1),
				(rrand(-1, 1) + 0.2)
					.linlin(-1.0, 1.0, -1.0, 1.0, clip: \minmax),
				reps),

			\amp, Env(
				[0, 0.1, 0],
				[rrand(4, 10), rrand(6, 14)],
				\sin),

			\cutoff, Env(
				[cutoff, cutoff * 4, cutoff / 2],
				[rrand(8, 12), rrand(8, 10)],
				\sin),

			\out, Pseq([
				(~reverbLongBus[rrand(0, 4) % ~numPairs]),
				(~reverbShortBus[rrand(0, 4) % ~numPairs]!4)
			], inf),

			{
				{
					buf.do {
						|b|
						b.free;
					}
				}.defer(40);
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
			.linlin(0, 10000, 1, 10, clip: \minmax),
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 0.0, 0.7, clip: \minmax),
		atk = 0.1;


		Pkill(

			\instrument, \playback,

			\env, 1,

			\atk, atk,

			\rel, (~sampleSize * 2) - atk,

			\dur, Pwhite(0.01, 3.0),

			\rate, rate,

			\rel, 0.5 * (1 / rate),

			\cutoff, Env(
				[freq * 2000, freq * 1000, freq * 500],
				[rrand(0.1, 0.5), rrand(1, 9)],
				\sin),

			\buf, Pshuf(buf, inf),

			\pan, Pwhite(-0.2, 0.2),

			\amp, Pwhite(0.01, 0.1),

			\out, Pseq([
				(~dryBus[rrand(0, 4) % ~numPairs]!2),
				(~reverbShortBus[4 % ~numPairs]!1),
				(~reverbMidBus[3 % ~numPairs]!(freq.asInteger)),
			], rrand(1, 3)),

			{
				{
					buf.do {
						|b|
						b.free;
					}
				}.defer(20);
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
		velocity = args.at(\avgrms).linlin(0.0, 1.0, 1.0, 3.0, clip: \minmax),
		reps = rrand(2, 25).asInteger,
		atk = 0.1;

		Pkill(

			\instrument, \playback,

			\env, 1,

			\dur, Pwhite(0.01, 0.1),

			\atk, atk,

			\rel, (~sampleSize * 2) - atk,

			\rate, Pseq([
				(0.5!3),
				(-0.5!(bright.asInteger))
			], inf),

			\cutoff, Env(
				[bright * 1000, bright * 2000, bright * 500],
				[rrand(0.1, 3), rrand(1, 19)],
				\sin),

			\buf, Pshuf(buf, inf),

			\pan, Pwhite(-1 * (freq / 2), freq / 2),

			\amp, Pwhite(0.01, 0.08),

			\out, Pseq([
				(~dryBus[3 % ~numPairs]!(resonant.asInteger)),
				(~reverbShortBus[2 % ~numPairs]!(resonant.asInteger)),
				(~reverbMidBus[rrand(0, 4) % ~numPairs]!2),
			], reps),

			{
				{
					buf.do {
						|b|
						b.free;
					}
				}.defer(20);
			}

		).play
	}

	granPlayer { // granular synthesis engine corresponding to input frequency


		|buf|

		var freq = args.at(\mainpitch).asString.pitchcps,
		choose = rrand(0, 4),
		dense = args.at(\density).linlin(0.0, 5.0, 5, 1, clip: \minmax),
		cent = args.at(\avgcent).linlin(0, 10000, 200, 16000, clip: \minmax),
		reps = rrand(1, 5),
		atk = 0.1;

		if ((freq < 30) || (choose < 2), {
			freq = rrand(2, 45)
		});

		Pkill(

			\instrument, \playgran,

			\dur, Pwhite(1, dense, inf),

			\env, 1,

			\buf, Pshuf(buf, inf),

			\atk, Pwhite(atk, atk * 13),

			\rel, (~sampleSize * 2) - atk,

			\freq, freq,

			\rate, Pseq([
				(0.5!(1)),
				(1.0!(1)),
				(-2.0!(1))
			], reps),

			\cutoff, Env(
				[cent, cent * 5, cent / 2],
				[(reps / 2).asInteger, (reps / 2).asInteger],
				\sin),

			\pan, Pwhite(-1, 1),

			\amp, Pwhite(0.01, 0.08),

			\out, Pseq([
				(~reverbShortBus[rrand(0, 4) % ~numPairs]),
				(~reverbMidBus[rrand(0, 4) % ~numPairs]),
				(~reverbLongBus[rrand(0, 4) % ~numPairs]),
			], inf),

			{
				{
					buf.do {
						|b|
						b.free;
					}
				}.defer(60);
			}

		).play
	}

	stateMachine { // function that relays handler buffer into playback machines

		|buf|

		//args.postln;

		//state = \an;

		("OK: Playing State: " + state + ".").postln;

		switch(state,

			\sc, {
				this.sciarrinoPlayer(buf)
			},

			\so, {
				this.sonorousPlayer(buf)
			},

			\ma, {
				this.machinePlayer(buf)
			},

			\te, {
				this.techPlayer(buf)
			},

			\an, {
				this.angelPlayer(buf)
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

		//msg.postln;

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

					{
						this.stateMachine(buf)
					}.defer(1);
				});
			})
		};
	}

}
