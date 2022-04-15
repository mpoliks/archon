(

~detect = false;
~lPeak = 0.25;
~thresh = 0.25;

~hardwareInputs = 1;
~hardwareOutputs = 2;

ServerOptions.devices;
Server.default.options.numInputBusChannels = ~hardwareInputs;
Server.default.options.numOutputBusChannels = ~hardwareOutputs;
s.options;
s.reboot;

SynthDef(\audioIn, {

	|out = 0,
	in = 0|

	var micIn;
	micIn = SoundIn.ar(in, 1);
	Out.ar(out, micIn)

}).add;


SynthDef(\inputAnalysis, {

	|in = 0|

	var input = In.ar(in),
	peak = Amplitude.ar(input),
	amp = RMS.ar(input).ampdb,
	freq = Pitch.kr(input),
	chain = FFT(LocalBuf(2048), input),
    centroid = SpecCentroid.kr(chain),
	flat = SpecFlatness.kr(chain),
	rolloff = SpecPcile.kr(chain, 0.5),
	trig = Impulse.kr(20);

	SendReply.kr(trig, '/analysis',[
		peak,
		amp,
		freq[0].cpsmidi,
		freq[1],
		centroid,
		flat,
		rolloff

	],
		in);

}).add;

~envDetect = {

	|peak = 0|

	if (~detect == false,
		{

		if (peak > (~thresh * 1.15), {
			~detect = true;
			~lPeak = 0.25;
			~detect.postln;
			});
		},

		{
		if (peak < ~thresh, {
			~detect = false;
			~detect.postln;
				}, {
			if (peak > ~lPeak, {
				~lPeak = peak;
				~thresh = ~lPeak / 2;
				if (~thresh < 0.25, ~thresh = 0.25);
			});
		});
	});
};

)

(
~analysisBus = Bus.audio(s, 1);
~inputGroup = Group.tail(s);
~analysisGroup = Group.tail(s);


Synth(
	\audioIn, [
		\out, ~analysisBus.index,
		\in, 0
	],
		~inputGroup
);


Synth(
	\inputAnalysis, [
		\in, ~analysisBus.index
	],
	~analysisGroup
);


OSCdef(\listener, {

	|msg|

	var peak = msg[3];

	~envDetect.(peak);


	// msg.postln;


}, '/analysis');
)