
var c = require('../config');

/*
 * GET /
 */
exports.index = function(req, res) {
    res.render('index', {
        config: c.config,
        mode: 'mobile'
    });
};

/*
 * GET /pet
 */
exports.pet = function(req, res) {
    res.render('index', {
        config: c.config,
        mode: 'pet'
    });
};
