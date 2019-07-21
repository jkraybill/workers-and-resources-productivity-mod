function Industry(name, color, linecolor) {
  this.name = name;
  var productivityMean = parseInt($("#industryMean").val());
  var productivitySd = parseInt($("#industrySd").val());
  this.productivity = Math.max(0.1, gaussian(productivityMean / 100, productivitySd / 100));
  this.size = Math.max(0.1, gaussian(productivityMean / 100, productivitySd / 100));
  this.fame = Math.min(1.8, Math.max(0.4, this.size * this.productivity));
  this.efficiency = this.productivity / this.fame;
  this.color = color;
  this.linecolor = linecolor;
}
