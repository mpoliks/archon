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
				// event.postln;
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