const mongoose = require('mongoose');

// Schema for individual set logs (weight, reps per set)
const SetLogSchema = new mongoose.Schema({
    setNumber: { type: Number, required: true },
    reps: { type: Number, required: true },
    weight: { type: Number, required: true }
});

// Schema for each exercise performed in the session
const WorkoutExerciseLogSchema = new mongoose.Schema({
    exerciseId: { type: String, required: true }, // Reference to Exercise
    sets: [
        {
            setNumber: { type: Number, required: true },
            reps: { type: Number, required: true },
            weight: { type: Number, required: true }
        }
    ],
    lastUsedWeight: { type: Number, default: 0 },
    lastUsedReps: { type: Number, default: 0 }
});

// Schema for a full workout session
const WorkoutSessionSchema = new mongoose.Schema({
    userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true }, // Reference to User
    sessionDate: { type: Date, default: Date.now }, // Date workout was completed
    workoutName: { type: String, required: true },
    exerciseLogs: [WorkoutExerciseLogSchema] // List of exercises performed
});

// Export the models
module.exports = mongoose.model('WorkoutSession', WorkoutSessionSchema);
