Pkill : Pattern {
	var <>patternpairs;
	*new { arg ... pairs;
		// if (pairs.size.odd, { Error("Pbind should have even number of args.\n").throw; });
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

		addr = netAddr;

		"OK: Analysis Initialized".postln;

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
	}

	offsetFunctions {

		var cent = this.getMetric('cent'),
		rolloff = this.getMetric('rolloff'),
		flat = this.getMetric('flat'),
		rms = this.getMetric('rms'),
		pitch = this.getPitch(),
		str = [ctr, cent, flat, rolloff, rms, pitch].archonJSON;
		"OK: Offset Detected".postln;
		str.postln;

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

	*new {

        ^super.new.init()
	}


	init {

		"OK: Handler Initialized".postln;

	}


	patternPlayer {

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
					"dying".postln;
					buf.do {
						|b|
						Buffer.free(b);
						(b.asString + "freed ").postln;
					}
				}
			)
		).play
	}

	msg_handler {
		|msg, server|

		var buf = List.new();

		"msg received by handler".postln;

		msg.postln;

		msg.size.do {

			|i|

			var b;

			if (i < (msg.size - 1), {
				b = Buffer.read(server, msg[i], action: {
					"reading buffers".postln;
					buf.add(b);
					})
				},
				{
				b = Buffer.read(server, msg[i], action: {
					buf.add(b);
					"buffers read".postln;
					this.patternPlayer(buf);
				});
			});
		};
	}

}
	