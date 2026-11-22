component tremolo_plus
{
	audio_inport in;
	audio_inport f32 rate = 0.5;
	audio_inport f32 depth = 0.5;
	audio_inport f32 beats_per_frame = 0.0;
	value_inport i32 shape;
	value_inport i32 timebase;
	value_inport bool retrigger;
	event_inport NoteEvent note_in
	{
		if (note_in.type == note_in.NOTE_ON)
		{
			last_key = note_in.key;
			if (retrigger)
				pending_reset = true;
		}
	}
	audio_outport out;
	f32 phase = 0.0;
	f32 hold_a = 0.5;
	f32 hold_b = 0.5;
	i32 seed = 22222;
	i32 last_key = 69;
	bool pending_reset = false;
	f32 next_random()
	{
		seed = ((seed).i64 * 48271 % 2147483647).i32;
		return (seed).f32 / 2147483647.0;
	}
	f32 beats_for(i32 i)
	{
		if (i == 2)
			return 4.0;
		if (i == 3)
			return 2.0;
		if (i == 4)
			return 1.0;
		if (i == 5)
			return 0.5;
		if (i == 6)
			return 0.25;
		if (i == 7)
			return 0.125;
		if (i == 8)
			return 3.0;
		if (i == 9)
			return 1.5;
		if (i == 10)
			return 0.75;
		if (i == 11)
			return 0.375;
		if (i == 12)
			return 0.1875;
		if (i == 13)
			return 1.3333333;
		if (i == 14)
			return 0.6666667;
		if (i == 15)
			return 0.3333333;
		if (i == 16)
			return 0.1666667;
		return 0.0833333;
	}
	process
	{
		f32 r = fmax(0.0, fmin(1.0, rate));
		f32 multiplier = 0.02 * pow(2500.0, r);
		f32 sr = (EC.samplerate).f32;
		f32 cycles_per_frame = 0.0;
		if (timebase == 0)
			cycles_per_frame = multiplier / sr;
		else
			if (timebase == 1)
				cycles_per_frame = multiplier * 1000.0 / sr;
			else
				if (timebase == 18)
					cycles_per_frame = 440.0 * pow(2.0, ((last_key).f32 - 69.0) / 12.0) * multiplier / sr;
				else
					if (timebase == 19)
						cycles_per_frame = 0.0;
					else
						cycles_per_frame = beats_per_frame * multiplier / beats_for(timebase);
		if (pending_reset)
		{
			phase = 0.0;
			pending_reset = false;
		}
		phase = phase + cycles_per_frame;
		if (phase >= 1.0)
		{
			phase = phase - floor(phase);
			hold_a = hold_b;
			hold_b = next_random();
		}
		i32 which = shape;
		f32 lfo = 1.0;
		if (which == 0)
			lfo = 0.5 + 0.5 * sin(6.2831853 * phase);
		else
			if (which == 1)
				lfo = 1.0 - fabs(2.0 * phase - 1.0);
			else
				if (which == 2)
					lfo = (phase < 0.5) ? (1.0) : (0.0);
				else
					if (which == 3)
						lfo = phase;
					else
						if (which == 4)
							lfo = 1.0 - phase;
						else
							if (which == 5)
								lfo = hold_b;
							else
							{
								f32 s = phase * phase * (3.0 - 2.0 * phase);
								lfo = hold_a + (hold_b - hold_a) * s;
							}
		f32 amount = fmax(0.0, fmin(1.0, depth));
		f32 gain = 1.0 - amount * (1.0 - lfo);
		out = in * gain;
	}
}
