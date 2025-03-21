const mongoose = require('mongoose');

// Exercise Schema
const ExerciseSchema = new mongoose.Schema({
    _id: { type: String, required: true }, // Use exercise ID (e1) etc.
    name: { type: String, required: true },
    targetMuscles: [{ type: String, required: true }], // Array of target muscles
    exerciseType: { type: String, required: true }, // push/pull/legs
    equipment: { type: String, required: true },
    mechanics: { type: String, required: true }, // compound/isolation
    instructions: { type: String, required: true } // Exercise instructions
});

// Export the Exercise model
module.exports = mongoose.model('Exercise', ExerciseSchema);
