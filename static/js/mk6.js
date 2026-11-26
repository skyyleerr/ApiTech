'use strict';
document.querySelector('#menu-toggle')?.addEventListener('click',()=>document.querySelector('#sidebar').classList.toggle('open'));
document.querySelectorAll('form[data-confirm]').forEach(f=>f.addEventListener('submit',e=>{if(!confirm(f.dataset.confirm))e.preventDefault()}));
document.querySelectorAll('.menu a').forEach(a=>{if(a.pathname===location.pathname)a.parentElement.classList.add('active')});
function plot(canvas,series){
 const ctx=canvas.getContext('2d'),w=canvas.clientWidth||320,h=210,scale=devicePixelRatio||1;
 canvas.width=w*scale;canvas.height=h*scale;ctx.scale(scale,scale);ctx.clearRect(0,0,w,h);
 const colors=['#d49c00','#3b82f6','#10b981','#ab47bc','#ef6c00','#00838f'];
 const all=series.flatMap(s=>s.puntos);ctx.font='12px Segoe UI';ctx.fillStyle='#777';
 if(!all.length){ctx.fillText('Sin lecturas para este período',24,90);return}
 let min=Math.min(...all.map(x=>x.valor)),max=Math.max(...all.map(x=>x.valor));if(min===max){min-=1;max+=1}
 const times=all.map(x=>new Date(x.fecha).getTime()),tmin=Math.min(...times),tmax=Math.max(...times);
 const left=48,right=w-12,top=16,bottom=150;
 for(let i=0;i<4;i++){let y=top+(bottom-top)*i/3;ctx.strokeStyle='#ececec';ctx.beginPath();ctx.moveTo(left,y);ctx.lineTo(right,y);ctx.stroke();ctx.fillStyle='#666';ctx.fillText((max-(max-min)*i/3).toFixed(1),2,y+4)}
 series.forEach((s,i)=>{ctx.strokeStyle=colors[i%colors.length];ctx.lineWidth=2;ctx.beginPath();s.puntos.forEach((p,j)=>{const x=left+(new Date(p.fecha).getTime()-tmin)/(tmax-tmin||1)*(right-left),y=bottom-(p.valor-min)/(max-min)*(bottom-top);if(j)ctx.lineTo(x,y);else ctx.moveTo(x,y)});ctx.stroke();s.puntos.forEach(p=>{const x=left+(new Date(p.fecha).getTime()-tmin)/(tmax-tmin||1)*(right-left),y=bottom-(p.valor-min)/(max-min)*(bottom-top);ctx.fillStyle=colors[i%colors.length];ctx.beginPath();ctx.arc(x,y,2,0,Math.PI*2);ctx.fill()});if(i<3){ctx.fillStyle=colors[i%colors.length];ctx.fillText((s.sensor+' · '+s.origen).slice(0,30),left+(i%2)*130,183+Math.floor(i/2)*16)}});
 ctx.fillStyle='#777';ctx.fillText(new Date(tmin).toLocaleTimeString('es-CO',{hour:'2-digit',minute:'2-digit'}),left,168);ctx.fillText(new Date(tmax).toLocaleTimeString('es-CO',{hour:'2-digit',minute:'2-digit'}),Math.max(left,right-45),168);
 canvas.title=series.map(s=>s.sensor+': '+s.puntos.map(p=>p.valor+' '+s.unidad+' ('+p.fecha+')').join(', ')).join('\n');
}
document.querySelectorAll('[data-chart-url]').forEach(box=>{
 let cached=[];const status=box.querySelector('.chart-status');
 async function load(){try{const url=box.dataset.chartUrl+location.search;const r=await fetch(url,{headers:{Accept:'application/json'}});if(!r.ok)throw Error();const body=await r.json();cached=body.series;
(body.resumen||[]).forEach(r=>{const tile=document.querySelector('[data-summary-tipo="'+r.tipo+'"]');if(!tile)return;
tile.querySelector('.summary-value').textContent=r.valor===null?'—':Number(r.valor).toLocaleString('es-CO',{minimumFractionDigits:2,maximumFractionDigits:2})+' '+r.unidad;
tile.querySelector('.summary-count').textContent='Promedio de últimas lecturas recientes · '+r.muestras+' sensores';
tile.querySelector('.summary-date').textContent=r.fecha?'Última lectura: '+new Date(r.fecha).toLocaleString('es-CO')+(r.muestras?'':' · Datos desactualizados'):'Sin lecturas recibidas';
tile.querySelector('.summary-origin').textContent=r.origenes||'Sin origen registrado';});box.querySelectorAll('canvas').forEach(c=>plot(c,cached.filter(s=>s.tipo===c.dataset.tipo)));status.textContent='Consulta actualizada: '+new Date().toLocaleTimeString()+' · hasta 120 lecturas por sensor.'}catch(e){status.textContent='No se pudo actualizar la consulta. Se conservan los datos mostrados.'}}
 load();setInterval(load,30000);window.addEventListener('resize',()=>box.querySelectorAll('canvas').forEach(c=>plot(c,cached.filter(s=>s.tipo===c.dataset.tipo))));
});
