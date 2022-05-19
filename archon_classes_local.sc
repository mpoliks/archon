Pkill : Pattern {
	var <>patternpairs;
	*new { arg ... pairs;
		^super.newCopyArgs(pairs)
	}

	storeArgs { ^patternpairs }
	embedInStream { arg inevent;
		var event;
		var sawNil = false;
		var streampairs = patternpairs.copy;
		var endval = streampairs.size - 2;

		forBy (1, endval, 2) { arg i;
			streampairs.put(i, streampairs[i].asStream);
		};

		loop {
			if (inevent.isNil) { ^nil.yield };
			event = inevent.copy;
			forBy (0, endval, 2) { arg i;
				var name = streampairs[i];
				var stream = streampairs[i+1];
				var streamout = stream.next(event);
				event.postln;
				if (streamout.isNil) {
					this.kill(streampairs[endval + 1]);
					^inevent };

				if (name.isSequenceableCollection) {
					if (name.size > streamout.size) {
						("the pattern is not providing enough values to assign to the key set:" + name).warn;
						^inevent
					};
					name.do { arg key, i;
						event.put(key, streamout[i]);
					};
				}{
					event.put(name, streamout);
				};

			};
			inevent = event.yield;
		};
	}

	kill {
		|killed|
		killed.value(killed);
	}
}


Analysis {

	var
	detect = false,

	sr = 0,
	ctr = 0,
	tick = 0,
	addr,

	onsetList,
	offsetList,

	thresh = 0.01,
	lPeak = 0.01,

	dict;

	*new {
		|r, netAddr|
        ^super.new.init(r, netAddr)
	}


	init {

		|r, netAddr|

		sr = 1000 / r;

		onsetList = List.new();
		offsetList = List.new();

		addr = netAddr;

		"OK: Analysis Initialized".postln;

	}

	calculateDensity {

		var density, windowsize;

		if (onsetList.size > 1, {

			if (onsetList.size > 10, {

				onsetList = onsetList[1..onsetList.size];
				offsetList = offsetList[1..offsetList.size];
			});

			density = Array.fill(onsetList.size - 1, {
				|i|
				onsetList[i + 1] - onsetList[i]}).mean;

			windowsize = Array.fill(onsetList.size, {
				|i|
				offsetList[i] - onsetList[i]}).mean;

		},
		{
			density = 0;
			windowsize = 0;
		});

	^ [density, windowsize];
	}


	envDetect {

		|msg|

		var peak = msg[3];

		if (detect == false,
			{

				if (peak > (thresh * 1.15), {
					detect = true;
					this.onsetFunctions();
				});
			},

			{
				if (((peak < thresh) && (dict.values.size > 0)), {
					detect = false;
					this.offsetFunctions();
				},
			{
				if (peak > lPeak, {
						lPeak = peak;
						thresh = lPeak / 2;
						if (thresh < 0.05, thresh = 0.05);
					});
				this.analysisFunctions(msg);
			});
		});
	}

	onsetFunctions {

		lPeak = 0.25;
		dict = Dictionary.new;
		tick = 0;
		"OK: Onset Detected".postln;
		onsetList.add(Date.getDate.rawSeconds);

	}

	offsetFunctions {

		var cent = this.getMetric('cent'),
		rolloff = this.getMetric('rolloff'),
		flat = this.getMetric('flat'),
		rms = this.getMetric('rms'),
		pitch = this.getPitch(),
		str = [ctr, cent, flat, rolloff, rms, pitch].archonJSON,
		density, windowsize, calc;
		"OK: Offset Detected".postln;
		str.postln;

		offsetList.add(Date.getDate.rawSeconds);

		calc = this.calculateDensity();

		density = calc[0];
		windowsize = calc[1];

		NetAddr.localAddr.sendMsg("/behavior", density, windowsize, cent, rolloff, flat, rms, pitch);


		addr.sendMsg("/test", str);
		ctr = ctr + 1;





	}

	analysisFunctions {

		|msg|

		dict.putAll(
			Dictionary[("a" ++ tick.asString) ->
				Dictionary[
					\cent -> msg[7],
					\flat -> msg[8],
					\rolloff -> msg[9],
					\rms -> msg[4]
					]
				]
			);

		 if (msg[6].asFloat > 0.0, {
			dict.at(("a" ++ tick.asString)).putAll(Dictionary[\pitch -> msg[5].cpsmidi]);
			});

		dict.at("a" ++ tick.asString).postln;

		tick = tick + 1;

	}


	getPitch {

		var plist = List.new(),
		pitch;

		dict.values.size.do {
			|i|
			if (dict.values[i].at('pitch').notNil == true, {
				plist.add(dict.values[i].at('pitch'));
			})
		};

		if (plist.size > (dict.values.size / 2), {
			pitch = plist.median.midipitch;
		},
		{
			pitch = "unpitched";
		});

		^ pitch;

	}

	getMetric {

		|metric|

		var result;

		result = Array.fill(
			dict.values.size,{
				|i| dict.values[i].at(metric)
			}).median;

		^ result;

	}

}



Handler {

	var state = "halftime";

	*new {

        ^super.new.init()
	}


	init {

		"OK: Handler Initialized".postln;

	}

	switchstate {

		|args|

		state = args;

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
	