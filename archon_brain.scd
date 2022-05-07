(

~netAddr = NetAddr("127.0.0.1", 5005);

~hardwareInputs = 1;
~hardwareOutputs = 2;
~fr = 10;

ServerOptions.devices;
Server.default.options.numInputBusChannels = ~hardwareInputs;
Server.default.options.numOutputBusChannels = ~hardwareOutputs;
s.options;
s.reboot;
s.meter;

SynthDef(\audioIn, {

	|out = 0,
	in = 0|

	var micIn = SoundIn.ar(in, 1);

	Out.ar(out, micIn)

}).add;

SynthDef(\inputAnalysis, {

	|in = 0|

	var input = In.ar(in),
	peak = Amplitude.ar(input),
	amp = RMS.ar(input),
	freq = Pitch.kr(input),
	chain = FFT(LocalBuf(2048), input),
    centroid = SpecCentroid.kr(chain),
	flat = SpecFlatness.kr(chain),
	rolloff = SpecPcile.kr(chain, 0.5),
	trig = Impulse.kr(~fr);


	SendReply.kr(trig,
		'/analysis',[
			peak,
			amp,
			freq[0],
			freq[1],
			centroid,
			flat,
			rolloff
			],
		in);


}).add;

SynthDef(\playback, {

	|out = 0,
	rate = 1,
	atk = 0.1,
	rel = 0.4,
	pan = 0,
	buf,
	done,
	amp = 1.0|

	var sig, env;

	sig = PlayBuf.ar(
			1,
			buf,
			BufRateScale.ir(buf) * rate,
			doneAction: 2);
	env = EnvGen.kr(Env.new([0,1,0],[atk, rel],[1, -1]),doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	sig = (sig * env);
	Out.ar(out, sig);

}).add;

SynthDef(\verb, {
	|in,
	out,
	mix = 0.25,
	room = 0.15,
	damp = 0.5|

	var dry;

	dry = In.ar(in, 2);

    Out.ar(out,
        FreeVerb.ar(dry,
            mix,
            room,
			damp)
	);
}).add;

SynthDef(\eliVerb, {
	|in,
	predelay = 1,
	revtime = 10,
	lpf = 4500,
	mix = 0.5,
	amp = 1,
	out = 0|
	var dry, wet, temp, sig;

	dry = In.ar(in, 2);
	temp = In.ar(in, 2);
	wet = 0;
	temp = DelayN.ar(temp, 0.2, predelay);
	16.do {
		temp = AllpassN.ar(
			temp,
			0.05,
			{Rand(0.001, 0.05)}!2,
			revtime);
		temp - LPF.ar(temp, lpf);
		wet = wet + temp;
	};
	sig = XFade2.ar(dry,wet,mix * 2 - 1, amp);
	Out.ar(out,sig);
}).add;

)

(

~analysisBus = Bus.audio(s, 1);
~reverbShortBus = Bus.audio(s,2);
~reverbMidBus = Bus.audio(s,2);
~reverbLongBus = Bus.audio(s,2);

~inputGroup = Group.tail(s);
~analysisGroup = Group.tail(s);
~fxGroup = Group.tail(s);
~dsp = Analysis.new(~fr, ~netAddr);


Synth(
	\audioIn, [
		\out, ~analysisBus.index,
		\in, 0
	], ~inputGroup
);


Synth(
	\inputAnalysis, [
		\in, ~analysisBus.index
	], ~analysisGroup
);

~reverbShortSynth = Synth(
	\verb, [
		\in, ~reverbShortBus,
		\room, 0.5,
		\damp, 0.2,
		\lpf, 10000
	], ~fxGroup
);


~reverbMidSynth = Synth(
	\verb, [
		\in, ~reverbMidBus,
		\room, 2.0,
		\damp, 0.6,
		\lpf, 15000,
	], ~fxGroup
);


~reverbLongSynth = Synth(
	\eliVerb, [
		\in, ~reverbLongBus,
		\revtime, 13,
		\predelay, 1.0,
		\lpf, 3000,
	], ~fxGroup
);

)

(
OSCdef(
	\listener, {

	|msg|

	~dsp.envDetect(msg);

}, '/analysis');


OSCdef(
	\pythonlistener, {

	|msg|

	msg.postln;

}, '/superInterface');


OSCdef(
	\nodelistener, {

	|msg|

	var buf = List.new();

	msg.postln;

	msg.size.do {
		|i|
		var b;
		if (i != 0, {
				if (i < (msg.size - 1), {
				b = Buffer.read(s, msg[i], action: {
					buf.add(b);
					})
				},
				{
				b = Buffer.read(s, msg[i], action: {
					buf.add(b);
					buf.postln;
					Pdef(
						\rhythm, Pbind(
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
							(~reverbMidBus!2)], inf)
							)
						).play
					});
				});
			});
		};

}, '/node');


)
