function loadData() {
	fetch('/rest/metrics/sources')
		.then((res) => res.json())
		.then((res) => renderData(res));
}

function renderData(res) {
	var html="<table>";
	html+="<thead><tr><th>Source</th><th>Metric</th><th>Value</th><th>Unit</th><th>Last updated</th></tr></thead>";
	
	html+="<tbody>";
	for (let i = 0; i < res.length; i++) {
		var data = res[i];
		html+="<tr>";
		html+="<td>"+data.sourceId+"</td>";
		html+="<td>"+data.metricId+"</td>";
		html+="<td style='text-align:right'>"+data.value+"</td>";
		html+="<td>"+data.unit+"</td>";
		html+="<td>"+data.lastUpdated+"</td>";
		html+="</tr>";
	}
	html+="</tbody>";
	
	html+="</table>";
	
	document.getElementById("data").innerHTML=html;
	
}

loadData();

setInterval(loadData, 5000);