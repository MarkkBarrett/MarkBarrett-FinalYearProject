const mongoose = require('mongoose');
const { Schema } = mongoose;

const FormResultSchema = new Schema({
    userId:    { type: String, required: true }, 
    exercise:  { type: String, required: true },
    accuracy:  { type: Number, required: true },
    feedback:  { type: [String], default: [] },
    timestamp: { type: Date, default: Date.now }
}, { collection: 'formResults' });  

module.exports = mongoose.model('FormResult', FormResultSchema);