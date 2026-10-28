import { Component } from '@angular/core';

@Component({
  selector: 'app-auth-layout',
  template: `
    <div class="auth-layout">
      <section class="story-panel" aria-labelledby="story-title">
        <div class="eyebrow">
          <span class="tiny-spark" aria-hidden="true">✳</span> CONEXÕES QUE VIRAM HISTÓRIAS
        </div>
        <h1 id="story-title">Grandes momentos.<br />Um novo <em>começo.</em></h1>
        <p class="story-description">
          Toda experiência começa com uma conexão.<br />Que bom ter você por aqui.
        </p>
        <div class="ticket-scene" aria-hidden="true">
          <div class="orbit orbit-one"></div>
          <div class="orbit orbit-two"></div>
          <span class="scene-spark">✳</span><span class="scene-cross">+</span>
          <div class="ticket ticket-back"></div>
          <div class="ticket ticket-front">
            <div class="ticket-top"><span>ingresso.</span><span>ADMIT ONE ↗</span></div>
            <div class="ticket-title">VIVA O<br />PRÓXIMO.</div>
            <div class="ticket-bottom">
              <span>SEU LUGAR É AQUI</span>
              <div class="barcode"></div>
            </div>
            <div class="ticket-stub">
              <span>BOAS HISTÓRIAS COMEÇAM AQUI</span><span>№ 000001</span>
            </div>
          </div>
          <div class="scene-note"><span>↗</span> Abra espaço para o novo.</div>
        </div>
        <div class="story-bottom"><span class="small-line"></span> O primeiro passo é seu.</div>
      </section>
      <section class="form-panel"><ng-content /></section>
    </div>
  `,
})
export class AuthLayout {}
