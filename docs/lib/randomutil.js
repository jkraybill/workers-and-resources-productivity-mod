function nextGaussian() {
    var u = 0, v = 0;
    while(u === 0) u = Math.random(); //Converting [0,1) to (0,1)
    while(v === 0) v = Math.random();
    return Math.sqrt( -2.0 * Math.log( u ) ) * Math.cos( 2.0 * Math.PI * v );
}

function gaussian(mean, stdevUnit) {
	return mean + (nextGaussian() * stdevUnit);
}

function getRandomInt(min, max) {
  return min + Math.floor(Math.random() * Math.floor(max - min));
}

function getRandomArrayElement(arrValues) {
	return arrValues[getRandomInt(0, arrValues.length - 1)];
}