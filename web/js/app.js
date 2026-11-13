/* =====================================================================
   Transporte Social — telas do sistema (HTML + CSS + JavaScript)
   Toda regra de negócio é conferida no back-end Java; aqui só chamamos
   a API (JSON) e mostramos o resultado.
   ===================================================================== */
'use strict';

const API = '/api';

let token = null;        // token da sessão (RNF01)
let usuario = null;      // usuário logado
let timer = null;        // atualização automática das listas

const NOME_DOC = {
  CPF: 'CPF',
  COMPROVANTE_RENDA: 'Comprovante de renda',
  ANTECEDENTES_CRIMINAIS: 'Certidão de antecedentes criminais',
  CNH: 'CNH (com observação EAR)'
};
const DOCS_POR_TIPO = {
  CIDADAO: ['CPF', 'COMPROVANTE_RENDA', 'ANTECEDENTES_CRIMINAIS'],
  MOTORISTA: ['CNH', 'ANTECEDENTES_CRIMINAIS']
};
const NOME_TIPO = { CIDADAO: 'Cidadão', MOTORISTA: 'Motorista', ADMIN: 'Administrador' };
const NOME_STATUS_CADASTRO = { PENDENTE: 'Pendente', APROVADO: 'Aprovado', RECUSADO: 'Recusado' };
const NOME_STATUS_VIAGEM = {
  SOLICITADA: 'Aguardando motorista',
  ACEITA: 'Motorista a caminho',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada'
};
const NOME_FORMA = { CREDITO: 'Cartão de crédito', DEBITO: 'Cartão de débito', PIX: 'Pix' };
const ATIVAS = ['SOLICITADA', 'ACEITA', 'EM_ANDAMENTO'];

/* =====================================================================
   Utilitários
   ===================================================================== */
const $ = (id) => document.getElementById(id);

function esc(texto) {
  if (texto === null || texto === undefined) return '';
  return String(texto)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function dinheiro(valor) {
  return Number(valor || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function dataHora(texto) {               // "2026-11-11 14:30" -> "11/11/2026 14:30"
  if (!texto) return '';
  const [d, h] = texto.split(' ');
  const [a, m, dia] = d.split('-');
  return `${dia}/${m}/${a}` + (h ? ` ${h}` : '');
}

function dataCurta(texto) {              // "1990-05-01" -> "01/05/1990"
  if (!texto) return '';
  const [a, m, d] = texto.split('-');
  return `${d}/${m}/${a}`;
}

function mascaraCpf(campo) {
  campo.addEventListener('input', () => {
    const n = campo.value.replace(/\D/g, '').slice(0, 11);
    let r = n;
    if (n.length > 9) r = `${n.slice(0, 3)}.${n.slice(3, 6)}.${n.slice(6, 9)}-${n.slice(9)}`;
    else if (n.length > 6) r = `${n.slice(0, 3)}.${n.slice(3, 6)}.${n.slice(6)}`;
    else if (n.length > 3) r = `${n.slice(0, 3)}.${n.slice(3)}`;
    campo.value = r;
  });
}

function mensagem(el, texto, tipo = 'erro') {
  if (!texto) { el.classList.add('hidden'); return; }
  el.className = `alerta alerta-${tipo}`;
  el.textContent = texto;
}

function toast(texto) {
  const t = $('toast');
  t.textContent = texto;
  t.classList.remove('hidden');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => t.classList.add('hidden'), 3500);
}

function guardar(chave, valor) {
  try {
    if (valor === null) sessionStorage.removeItem(chave);
    else sessionStorage.setItem(chave, valor);
  } catch (e) { /* navegador sem armazenamento: segue sem lembrar a sessão */ }
}

function recuperar(chave) {
  try { return sessionStorage.getItem(chave); } catch (e) { return null; }
}

/** Chamada à API em JSON com o token da sessão. */
async function api(metodo, caminho, corpo) {
  const opcoes = { method: metodo, headers: {} };
  if (token) opcoes.headers.Authorization = `Bearer ${token}`;
  if (corpo !== undefined) {
    opcoes.headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(corpo);
  }
  let resp;
  try {
    resp = await fetch(API + caminho, opcoes);
  } catch (e) {
    throw new Error('Não foi possível falar com o servidor. Ele está ligado?');
  }
  let dados = null;
  try { dados = await resp.json(); } catch (e) { /* resposta sem JSON */ }
  if (resp.status === 401 && token) {
    sairDoSistema('Sua sessão terminou. Entre novamente.');
    throw new Error('Sessão encerrada.');
  }
  if (!resp.ok) throw new Error(dados && dados.erro ? dados.erro : `Erro ${resp.status}`);
  return dados;
}

/** Lê um arquivo escolhido como Base64 (data URL). */
function lerArquivo(arquivo) {
  return new Promise((resolve, reject) => {
    const leitor = new FileReader();
    leitor.onload = () => resolve(leitor.result);
    leitor.onerror = () => reject(new Error(`Não foi possível ler o arquivo ${arquivo.name}.`));
    leitor.readAsDataURL(arquivo);
  });
}

/** Monta os campos de envio de documentos (RF02). */
function camposDocumentos(container, tipos, prefixo) {
  container.innerHTML = tipos.map((t) => `
    <div class="campo">
      <label for="${prefixo}-${t}">${esc(NOME_DOC[t])}</label>
      <input id="${prefixo}-${t}" type="file" accept=".pdf,.jpg,.jpeg,.png" data-tipo="${t}" />
    </div>`).join('');
}

/** Lê os arquivos escolhidos e devolve os campos doc_TIPO_nome / doc_TIPO_conteudo. */
async function lerDocumentos(container, obrigatorios) {
  const dados = {};
  for (const campo of container.querySelectorAll('input[type=file]')) {
    const tipo = campo.dataset.tipo;
    const arquivo = campo.files[0];
    if (!arquivo) {
      if (obrigatorios) throw new Error(`Envie o documento: ${NOME_DOC[tipo]}.`);
      continue;
    }
    if (!/\.(pdf|jpe?g|png)$/i.test(arquivo.name)) throw new Error(`${NOME_DOC[tipo]}: envie PDF, JPG ou PNG.`);
    if (arquivo.size > 5 * 1024 * 1024) throw new Error(`${NOME_DOC[tipo]}: o arquivo passa de 5 MB.`);
    dados[`doc_${tipo}_nome`] = arquivo.name;
    dados[`doc_${tipo}_conteudo`] = await lerArquivo(arquivo);
  }
  return dados;
}

/* =====================================================================
   Modal e confirmação
   ===================================================================== */
function abrirModal(titulo, html) {
  $('modal-titulo').textContent = titulo;
  $('modal-corpo').innerHTML = html;
  $('modal-fundo').classList.remove('hidden');
  const primeiro = $('modal-corpo').querySelector('input, select, button');
  if (primeiro) primeiro.focus();
}

function fecharModal() {
  $('modal-fundo').classList.add('hidden');
  $('modal-corpo').innerHTML = '';
}

function confirmar(titulo, texto, rotulo = 'Confirmar', classe = 'btn-primary') {
  return new Promise((resolve) => {
    abrirModal(titulo, `
      <p>${esc(texto)}</p>
      <div class="campo-acoes" style="margin-top:16px">
        <button class="btn btn-secondary" id="conf-nao">Voltar</button>
        <button class="btn ${classe}" id="conf-sim">${esc(rotulo)}</button>
      </div>`);
    $('conf-nao').onclick = () => { fecharModal(); resolve(false); };
    $('conf-sim').onclick = () => { fecharModal(); resolve(true); };
  });
}

/* =====================================================================
   Login, cadastro e sessão
   ===================================================================== */
function mostrarTela(id) {
  ['tela-login', 'tela-cadastro', 'tela-app'].forEach((t) => $(t).classList.toggle('hidden', t !== id));
  window.scrollTo(0, 0);
}

async function fazerLogin(evento) {
  evento.preventDefault();
  const msg = $('login-msg');
  mensagem(msg, null);
  const cpf = $('login-cpf').value.trim();
  const senha = $('login-senha').value;
  if (!cpf || !senha) { mensagem(msg, 'Informe o CPF e a senha.'); return; }
  try {
    const r = await api('POST', '/login', { cpf, senha });
    token = r.token;
    guardar('token', token);
    $('login-senha').value = '';
    entrarNoSistema(r.usuario);
  } catch (e) {
    mensagem(msg, e.message);
  }
}

function sairDoSistema(aviso) {
  if (token) fetch(`${API}/logout`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } }).catch(() => {});
  token = null;
  usuario = null;
  guardar('token', null);
  clearInterval(timer);
  fecharModal();
  mostrarTela('tela-login');
  if (aviso) mensagem($('login-msg'), aviso, 'info');
}

function tipoCadastro() {
  return document.querySelector('input[name="tipo-cad"]:checked').value;
}

function ajustarCadastro() {
  const tipo = tipoCadastro();
  $('campos-cidadao').classList.toggle('hidden', tipo !== 'CIDADAO');
  $('campos-motorista').classList.toggle('hidden', tipo !== 'MOTORISTA');
  camposDocumentos($('cad-documentos'), DOCS_POR_TIPO[tipo], 'cad-doc');
}

/** RF01 + RF02 (+ RF05 para o motorista): envia o cadastro com os documentos. */
async function cadastrar(evento) {
  evento.preventDefault();
  const msg = $('cad-msg');
  const botao = $('btn-cadastrar');
  mensagem(msg, null);
  const tipo = tipoCadastro();
  const dados = {
    tipo,
    nome: $('cad-nome').value,
    cpf: $('cad-cpf').value,
    dataNascimento: $('cad-nascimento').value,
    email: $('cad-email').value,
    telefone: $('cad-telefone').value,
    senha: $('cad-senha').value
  };
  if (tipo === 'CIDADAO') {
    dados.rendaFamiliar = $('cad-renda').value;
    dados.nis = $('cad-nis').value;
  } else {
    Object.assign(dados, {
      cnh: $('cad-cnh').value,
      placa: $('cad-placa').value,
      marca: $('cad-marca').value,
      modelo: $('cad-modelo').value,
      ano: $('cad-ano').value,
      cor: $('cad-cor').value
    });
  }
  botao.disabled = true;
  botao.textContent = 'Enviando...';
  try {
    Object.assign(dados, await lerDocumentos($('cad-documentos'), true));
    await api('POST', '/usuarios', dados);
    $('form-cadastro').reset();
    ajustarCadastro();
    $('login-cpf').value = dados.cpf;
    mostrarTela('tela-login');
    mensagem($('login-msg'),
      'Cadastro enviado! A prefeitura vai conferir os seus documentos. Entre para acompanhar a análise.', 'ok');
  } catch (e) {
    mensagem(msg, e.message);
  } finally {
    botao.disabled = false;
    botao.textContent = 'Enviar cadastro';
  }
}

/* =====================================================================
   Entrada no sistema: cada perfil vê só a sua tela (RNF01)
   ===================================================================== */
const MENUS = {
  CIDADAO: [
    { ic: '🏠', texto: 'Painel', alvo: 'topo' },
    { ic: '📍', texto: 'Solicitar viagem', alvo: 'card-solicitar' },
    { ic: '🕘', texto: 'Histórico', alvo: 'card-historico' }
  ],
  MOTORISTA: [
    { ic: '🏠', texto: 'Painel', alvo: 'topo' },
    { ic: '📋', texto: 'Solicitações', alvo: 'card-abertas' },
    { ic: '🚗', texto: 'Meus veículos', alvo: 'card-veiculos' },
    { ic: '🕘', texto: 'Minhas corridas', alvo: 'card-corridas' }
  ],
  ADMIN: [
    { ic: '⏳', texto: 'Cadastros pendentes', aba: 'tab-pendentes' },
    { ic: '👥', texto: 'Usuários', aba: 'tab-usuarios' },
    { ic: '📊', texto: 'Relatório', aba: 'tab-relatorio' }
  ]
};

/** Menu lateral: cada perfil vê só as suas opções (RNF01). */
function montarMenu(u) {
  const aprovado = u.status === 'APROVADO' || u.tipo === 'ADMIN';
  const itens = MENUS[u.tipo].filter((i) => aprovado || i.alvo === 'topo' || i.alvo === 'card-veiculos');
  $('menu').innerHTML = itens.map((i, n) => `
      <button class="nav ${n === 0 ? 'ativo' : ''}" data-alvo="${i.alvo || ''}" data-aba="${i.aba || ''}"><span class="ic">${i.ic}</span>${i.texto}</button>`).join('')
    + '<button class="nav" id="btn-conta"><span class="ic">👤</span>Minha conta</button>'
    + '<button class="nav nav-sair" id="btn-sair"><span class="ic">↩</span>Sair</button>';
  $('menu').querySelectorAll('.nav[data-alvo], .nav[data-aba]').forEach((b) => {
    if (!b.dataset.alvo && !b.dataset.aba) return;
    b.addEventListener('click', () => {
      if (b.dataset.aba) { mudarAba(b.dataset.aba); return; }
      $('menu').querySelectorAll('.nav').forEach((x) => x.classList.toggle('ativo', x === b));
      if (b.dataset.alvo === 'topo') window.scrollTo({ top: 0, behavior: 'smooth' });
      else $(b.dataset.alvo).scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });
  $('btn-conta').addEventListener('click', abrirMinhaConta);
  $('btn-sair').addEventListener('click', () => sairDoSistema());
}

function desenharCabecalho(u) {
  const primeiroNome = (u.nome || '').split(' ')[0];
  $('perfil-avatar').textContent = primeiroNome.charAt(0).toUpperCase();
  $('perfil-nome').textContent = u.nome;
  $('perfil-tipo').textContent = `${NOME_TIPO[u.tipo]}${u.tipo === 'ADMIN' ? '' : ' · ' + NOME_STATUS_CADASTRO[u.status]}`;
  const textos = {
    CIDADAO: ['Para onde você precisa ir hoje?'],
    MOTORISTA: ['Acompanhe e atenda as solicitações de viagem.'],
    ADMIN: ['Análise de cadastros e gastos do programa.']
  };
  $('saudacao').textContent = u.tipo === 'ADMIN' ? 'Painel da prefeitura' : `Olá, ${primeiroNome} 👋`;
  $('subtitulo').textContent = textos[u.tipo][0];
  const tag = $('tag-situacao');
  if (u.tipo === 'ADMIN') {
    tag.className = 'tag badge-ADMIN';
    tag.textContent = 'Administrador';
  } else {
    tag.className = `tag badge-${u.status}`;
    tag.textContent = `Cadastro ${NOME_STATUS_CADASTRO[u.status].toLowerCase()}`;
  }
}

function entrarNoSistema(u) {
  usuario = u;
  mostrarTela('tela-app');
  desenharCabecalho(u);
  montarMenu(u);

  ['painel-cidadao', 'painel-motorista', 'painel-admin'].forEach((p) => $(p).classList.add('hidden'));
  mostrarSituacaoCadastro();

  clearInterval(timer);
  if (u.tipo === 'ADMIN') {
    $('painel-admin').classList.remove('hidden');
    mudarAba('tab-pendentes');
    prepararRelatorio();
    carregarIndicadoresAdmin();
  } else if (u.tipo === 'CIDADAO') {
    abrirPainelCidadao();
  } else {
    abrirPainelMotorista();
  }
  // Atualiza as listas sozinho, para acompanhar o status da viagem (RF10)
  timer = setInterval(atualizacaoAutomatica, 10000);
}

function atualizacaoAutomatica() {
  if (!usuario || usuario.status !== 'APROVADO' || !$('modal-fundo').classList.contains('hidden')) return;
  const foco = document.activeElement;
  if (foco && foco.tagName === 'SELECT') return;     // não atrapalha quem está escolhendo algo
  if (usuario.tipo === 'CIDADAO') carregarViagensCidadao();
  if (usuario.tipo === 'MOTORISTA') { carregarAbertas(); carregarCorridas(); }
}

/** RF03 / RF04 / RN04: mostra a situação do cadastro e bloqueia as funções de viagem. */
function mostrarSituacaoCadastro() {
  const caixa = $('aviso-cadastro');
  if (usuario.tipo === 'ADMIN' || usuario.status === 'APROVADO') {
    caixa.classList.add('hidden');
    caixa.innerHTML = '';
    return;
  }
  caixa.classList.remove('hidden');
  if (usuario.status === 'PENDENTE') {
    caixa.innerHTML = `
      <div class="aviso aviso-pendente">
        <h3>⏳ Cadastro em análise</h3>
        <p>A prefeitura está conferindo os seus documentos. As funções de viagem ficam bloqueadas até a aprovação.</p>
      </div>`;
    return;
  }
  caixa.innerHTML = `
    <div class="aviso aviso-recusado">
      <h3>❌ Cadastro recusado</h3>
      <p><strong>Motivo:</strong> ${esc(usuario.motivoRecusa)}</p>
      <p>Você pode refazer o cadastro enviando novos documentos. Ele volta para a análise da prefeitura.</p>
      <form id="form-reenvio" novalidate>
        <div id="reenvio-documentos"></div>
        <div id="reenvio-msg" class="alerta hidden"></div>
        <button type="submit" class="btn btn-primary">Enviar novos documentos</button>
      </form>
    </div>`;
  camposDocumentos($('reenvio-documentos'), usuario.documentosObrigatorios, 'reenvio-doc');
  $('form-reenvio').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const docs = await lerDocumentos($('reenvio-documentos'), false);
      if (Object.keys(docs).length === 0) throw new Error('Selecione pelo menos um documento.');
      usuario = await api('POST', '/conta/documentos', docs);
      toast('Documentos enviados. Seu cadastro voltou para análise.');
      entrarNoSistema(usuario);
    } catch (erro) {
      mensagem($('reenvio-msg'), erro.message);
    }
  });
}

/* =====================================================================
   CIDADÃO
   ===================================================================== */
function abrirPainelCidadao() {
  if (usuario.status !== 'APROVADO') return;          // RF04
  $('painel-cidadao').classList.remove('hidden');
  limparSimulacao();
  carregarViagensCidadao();
}

function limparSimulacao() {
  $('sol-preview').classList.add('hidden');
  $('btn-confirmar').disabled = true;
}

/** RF08: mostra total, subsídio e parte do cidadão antes de confirmar. */
async function calcularValor() {
  const msg = $('sol-msg');
  mensagem(msg, null);
  try {
    const r = await api('POST', '/viagens/simular', { distanciaKm: $('sol-km').value });
    $('sol-total').textContent = dinheiro(r.valorTotal);
    $('sol-subsidio').textContent = dinheiro(r.valorSubsidio);
    $('sol-paga').textContent = dinheiro(r.valorCidadao);
    $('sol-tempo').textContent = `~${r.tempoEstimadoMin} min`;
    $('sol-barra').style.width = `${(r.valorSubsidio / r.valorTotal) * 100}%`;
    $('sol-preview').classList.remove('hidden');
    $('btn-confirmar').disabled = false;
  } catch (e) {
    limparSimulacao();
    mensagem(msg, e.message);
  }
}

/** RF07: confirma a solicitação. */
async function solicitarViagem(evento) {
  evento.preventDefault();
  const msg = $('sol-msg');
  mensagem(msg, null);
  try {
    await api('POST', '/viagens', {
      origem: $('sol-origem').value,
      destino: $('sol-destino').value,
      distanciaKm: $('sol-km').value,
      qtdAcompanhantes: $('sol-menores').value || '0'
    });
    $('form-solicitar').reset();
    $('sol-menores').value = '0';
    limparSimulacao();
    toast('Viagem solicitada! Aguarde um motorista aceitar.');
    carregarViagensCidadao();
  } catch (e) {
    mensagem(msg, e.message);
  }
}

/** RF10 / RF12 / RF13: status, tempo estimado, limite da semana e histórico. */
async function carregarViagensCidadao() {
  const lista = $('lista-viagens-cidadao');
  try {
    const r = await api('GET', '/viagens/minhas');
    $('stat-restantes').textContent = r.restantesNaSemana;
    $('stat-restantes-txt').textContent = r.restantesNaSemana;
    $('stat-limite').textContent = r.limiteSemanal;
    $('anel-restantes').style.setProperty('--p', (r.restantesNaSemana / r.limiteSemanal) * 100);
    const atual = r.viagens.find((v) => ATIVAS.includes(v.status));
    desenharViagemAtual(atual);
    r.viagens = r.viagens.filter((v) => v !== atual);
    const bloqueado = r.restantesNaSemana <= 0;
    $('btn-calcular').disabled = bloqueado;
    if (bloqueado) {
      mensagem($('sol-msg'), `Você já usou as ${r.limiteSemanal} viagens desta semana. Novas solicitações na próxima segunda-feira.`, 'info');
    }
    if (r.viagens.length === 0) {
      lista.innerHTML = '<p class="vazio">Nenhuma viagem no histórico ainda.</p>';
      return;
    }
    lista.innerHTML = r.viagens.map(itemViagemCidadao).join('');
  } catch (e) {
    lista.innerHTML = `<p class="alerta alerta-erro">${esc(e.message)}</p>`;
  }
}

/** RF10: etapas da viagem, motorista e tempo estimado de chegada. */
function desenharViagemAtual(v) {
  const caixa = $('viagem-atual');
  const tag = $('tag-atual');
  if (!v) {
    caixa.innerHTML = '<p class="vazio">Nenhuma viagem em andamento.<br />Solicite uma viagem ao lado.</p>';
    tag.classList.add('hidden');
    $('kpi-atual').textContent = 'Nenhuma';
    $('kpi-atual-sub').textContent = 'Solicite uma viagem';
    return;
  }
  tag.className = `tag badge-${v.status}`;
  tag.textContent = NOME_STATUS_VIAGEM[v.status];
  $('kpi-atual').textContent = NOME_STATUS_VIAGEM[v.status];
  $('kpi-atual-sub').textContent = `Tempo estimado: ~${v.tempoEstimadoMin} min`;

  const etapas = ['SOLICITADA', 'ACEITA', 'EM_ANDAMENTO', 'CONCLUIDA'];
  const nomes = ['Solicitada', 'Aceita', 'Em viagem', 'Concluída'];
  const atual = etapas.indexOf(v.status);
  const passos = etapas.map((e, i) => {
    const classe = i < atual ? 'ok' : (i === atual ? (i === 0 ? 'ok' : 'atual') : '');
    const bola = classe === 'ok' ? '✓' : i + 1;
    const traco = i < etapas.length - 1 ? `<div class="traco ${i < atual ? 'ok' : ''}"></div>` : '';
    return `<div class="passo ${classe}"><div class="bola">${bola}</div>${nomes[i]}</div>${traco}`;
  }).join('');

  let pessoa;
  if (v.motoristaNome) {
    pessoa = `
      <div class="caixa-pessoa">
        <div class="avatar">${esc(v.motoristaNome.charAt(0).toUpperCase())}</div>
        <div><strong>${esc(v.motoristaNome)}</strong><small>Motorista credenciado</small></div>
        <div class="eta"><small>Tempo estimado</small><b>~${v.tempoEstimadoMin} min</b></div>
      </div>`;
  } else {
    pessoa = `
      <div class="caixa-pessoa">
        <div class="avatar">…</div>
        <div><strong>Procurando motorista</strong><small>Aguardando um motorista aceitar</small></div>
        <div class="eta"><small>Tempo estimado</small><b>~${v.tempoEstimadoMin} min</b></div>
      </div>
      <p class="aviso-espera">Sem motorista em 10 minutos, a solicitação é cancelada automaticamente, sem custo.</p>`;
  }
  const podeCancelar = v.status === 'SOLICITADA' || v.status === 'ACEITA';
  caixa.innerHTML = `
    <div class="trajeto">${esc(v.origem)} → <b>${esc(v.destino)}</b></div>
    <div class="passos">${passos}</div>
    ${pessoa}
    <div class="item-info" style="margin-top:12px">
      <span>📏 ${String(v.distanciaKm).replace('.', ',')} km</span>
      <span>👶 Menores: ${v.qtdAcompanhantes}</span>
      ${infoValores(v)}
    </div>
    ${podeCancelar ? `<button class="btn btn-perigo btn-block" style="margin-top:14px" onclick="cancelarViagem(${v.id})">Cancelar sem custo</button>` : ''}`;
}

function infoValores(v) {
  return `
    <span>Total: <strong>${dinheiro(v.valorTotal)}</strong></span>
    <span>Prefeitura: <strong>${dinheiro(v.valorSubsidio)}</strong></span>
    <span>Cidadão: <strong>${dinheiro(v.valorCidadao)}</strong></span>`;
}

function itemViagemCidadao(v) {
  const ativa = ATIVAS.includes(v.status);
  let situacao = '';
  if (v.status === 'SOLICITADA') {
    situacao = 'Aguardando um motorista aceitar. Sem motorista em 10 minutos, a solicitação é cancelada automaticamente.';
  } else if (v.status === 'ACEITA') {
    situacao = `Motorista <strong>${esc(v.motoristaNome)}</strong> aceitou e está a caminho.`;
  } else if (v.status === 'EM_ANDAMENTO') {
    situacao = `Em viagem com <strong>${esc(v.motoristaNome)}</strong>.`;
  }
  if (ativa) situacao += ` Tempo estimado de chegada: <strong>~${v.tempoEstimadoMin} min</strong>.`;

  let acoes = '';
  if (v.status === 'SOLICITADA' || v.status === 'ACEITA') {
    acoes = `<button class="btn btn-perigo btn-sm" onclick="cancelarViagem(${v.id})">Cancelar viagem</button>`;
  } else if (v.status === 'CONCLUIDA' && !v.pagamento) {
    acoes = `
      <select id="forma-${v.id}" aria-label="Forma de pagamento">
        <option value="PIX">Pix</option>
        <option value="DEBITO">Cartão de débito</option>
        <option value="CREDITO">Cartão de crédito</option>
      </select>
      <button class="btn btn-sucesso btn-sm" onclick="pagarViagem(${v.id})">Pagar ${dinheiro(v.valorCidadao)}</button>`;
  } else if (v.pagamento) {
    acoes = `<span class="badge badge-PAGO">Pago via ${esc(NOME_FORMA[v.pagamento.forma])} em ${dataHora(v.pagamento.dataHora)}</span>`;
  }

  return `
    <div class="item ${ativa ? 'item-ativo' : ''}">
      <div class="item-topo">
        <span class="item-titulo">${esc(v.origem)} → ${esc(v.destino)}</span>
        <span class="badge badge-${v.status}">${NOME_STATUS_VIAGEM[v.status]}</span>
      </div>
      ${situacao ? `<div class="destaque-status">${situacao}</div>` : ''}
      <div class="item-info">
        <span>📅 ${dataHora(v.dataHora)}</span>
        <span>📏 ${String(v.distanciaKm).replace('.', ',')} km</span>
        <span>👶 Menores: ${v.qtdAcompanhantes}</span>
        ${v.status === 'CANCELADA' ? '<span>Sem cobrança e sem subsídio</span>' : infoValores(v)}
      </div>
      ${acoes ? `<div class="item-acoes">${acoes}</div>` : ''}
    </div>`;
}

async function cancelarViagem(id) {
  if (!await confirmar('Cancelar viagem', 'Cancelar esta viagem? Não há cobrança enquanto o motorista não chegou ao embarque.', 'Cancelar viagem', 'btn-perigo')) return;
  try {
    await api('POST', `/viagens/${id}/cancelar`);
    toast('Viagem cancelada sem custo.');
  } catch (e) {
    toast(e.message);
  }
  carregarViagensCidadao();
}

/** RF11: pagamento da parte do cidadão (crédito, débito ou Pix). */
async function pagarViagem(id) {
  const forma = $(`forma-${id}`).value;
  try {
    const v = await api('POST', `/viagens/${id}/pagar`, { forma });
    toast(`Pagamento de ${dinheiro(v.pagamento.valor)} aprovado (${NOME_FORMA[forma]}).`);
  } catch (e) {
    toast(e.message);
  }
  carregarViagensCidadao();
}

/* =====================================================================
   MOTORISTA
   ===================================================================== */
async function abrirPainelMotorista() {
  $('painel-motorista').classList.remove('hidden');
  const aprovado = usuario.status === 'APROVADO';
  // RF04: sem aprovação, o motorista só mantém os dados do veículo
  ['card-disponibilidade', 'card-abertas', 'card-corridas'].forEach((c) => $(c).classList.toggle('hidden', !aprovado));
  desenharVeiculos();
  if (aprovado) {
    desenharDisponibilidade();
    carregarAbertas();
    carregarCorridas();
  }
}

function desenharDisponibilidade() {
  $('chk-disponivel').checked = !!usuario.disponivel;
  $('card-disponibilidade').classList.toggle('ligado', !!usuario.disponivel);
  $('titulo-disponivel').innerHTML = usuario.disponivel
    ? '<span class="pulso"></span>&nbsp; Você está disponível'
    : 'Você está indisponível';
  $('txt-disponivel').textContent = usuario.disponivel
    ? 'Recebendo as solicitações em aberto. Desligue para ficar indisponível.'
    : 'Ligue para ver e aceitar as solicitações de viagem.';
}

/** RF06 */
async function alterarDisponibilidade() {
  const desejado = $('chk-disponivel').checked;
  try {
    usuario = await api('PUT', '/motorista/disponibilidade', { disponivel: desejado });
  } catch (e) {
    toast(e.message);
  }
  desenharDisponibilidade();
  carregarAbertas();
}

/** RF05 */
function desenharVeiculos() {
  const lista = $('lista-veiculos');
  const veiculos = usuario.veiculos || [];
  if (veiculos.length === 0) {
    lista.innerHTML = '<p class="vazio">Nenhum veículo cadastrado.</p>';
    return;
  }
  lista.innerHTML = veiculos.map((v) => `
    <div class="item">
      <div class="item-topo">
        <span class="item-titulo">${esc(v.marca)} ${esc(v.modelo)} — ${esc(v.placa)}</span>
        <button class="btn btn-outline btn-sm" onclick="abrirVeiculo(${v.id})">Alterar</button>
      </div>
      <div class="item-info"><span>Ano: ${v.ano}</span><span>Cor: ${esc(v.cor || '-')}</span></div>
    </div>`).join('');
}

function abrirVeiculo(id) {
  const v = (usuario.veiculos || []).find((x) => x.id === id) || {};
  abrirModal(id ? 'Alterar veículo' : 'Cadastrar veículo', `
    <form id="form-veiculo" novalidate>
      <div class="grid-2">
        <div class="campo"><label for="vei-placa">Placa</label><input id="vei-placa" maxlength="8" value="${esc(v.placa)}" placeholder="ABC1D23" /></div>
        <div class="campo"><label for="vei-ano">Ano</label><input id="vei-ano" type="number" value="${esc(v.ano)}" /></div>
      </div>
      <div class="grid-2">
        <div class="campo"><label for="vei-marca">Marca</label><input id="vei-marca" maxlength="50" value="${esc(v.marca)}" /></div>
        <div class="campo"><label for="vei-modelo">Modelo</label><input id="vei-modelo" maxlength="50" value="${esc(v.modelo)}" /></div>
      </div>
      <div class="campo"><label for="vei-cor">Cor</label><input id="vei-cor" maxlength="30" value="${esc(v.cor)}" /></div>
      <div id="vei-msg" class="alerta hidden"></div>
      <div class="campo-acoes">
        <button type="button" class="btn btn-secondary" onclick="fecharModal()">Voltar</button>
        <button type="submit" class="btn btn-primary">Salvar</button>
      </div>
    </form>`);
  $('form-veiculo').addEventListener('submit', async (e) => {
    e.preventDefault();
    const dados = {
      placa: $('vei-placa').value, ano: $('vei-ano').value, marca: $('vei-marca').value,
      modelo: $('vei-modelo').value, cor: $('vei-cor').value
    };
    try {
      if (id) await api('PUT', `/motorista/veiculos/${id}`, dados);
      else await api('POST', '/motorista/veiculos', dados);
      usuario = await api('GET', '/conta');
      fecharModal();
      desenharVeiculos();
      toast('Veículo salvo.');
    } catch (erro) {
      mensagem($('vei-msg'), erro.message);
    }
  });
}

/** RF09: solicitações em aberto para o motorista disponível. */
async function carregarAbertas() {
  const lista = $('lista-abertas');
  try {
    const r = await api('GET', '/viagens/abertas');
    if (!r.disponivel) {
      lista.innerHTML = '<p class="vazio">Fique disponível para ver as solicitações.</p>';
      return;
    }
    if (r.viagens.length === 0) {
      lista.innerHTML = '<p class="vazio">Nenhuma solicitação em aberto agora.</p>';
      return;
    }
    lista.innerHTML = r.viagens.map((v) => `
      <div class="item item-ativo">
        <div class="item-topo">
          <span class="item-titulo">${esc(v.origem)} → ${esc(v.destino)}</span>
          <button class="btn btn-sucesso btn-sm" onclick="acaoMotorista(${v.id}, 'aceitar')">Aceitar</button>
        </div>
        <div class="item-info">
          <span>👤 ${esc(v.cidadaoNome)}</span>
          <span>📅 ${dataHora(v.dataHora)}</span>
          <span>📏 ${String(v.distanciaKm).replace('.', ',')} km</span>
          <span>👶 Menores: ${v.qtdAcompanhantes}</span>
          <span>Valor: <strong>${dinheiro(v.valorTotal)}</strong></span>
        </div>
      </div>`).join('');
  } catch (e) {
    lista.innerHTML = `<p class="alerta alerta-erro">${esc(e.message)}</p>`;
  }
}

/** RF13: histórico do motorista, com iniciar/finalizar as viagens aceitas. */
async function carregarCorridas() {
  const lista = $('lista-viagens-motorista');
  try {
    const r = await api('GET', '/viagens/minhas');
    if (r.viagens.length === 0) {
      lista.innerHTML = '<p class="vazio">Nenhuma corrida ainda.</p>';
      return;
    }
    lista.innerHTML = r.viagens.map((v) => {
      let acao = '';
      if (v.status === 'ACEITA') acao = `<button class="btn btn-primary btn-sm" onclick="acaoMotorista(${v.id}, 'iniciar')">Cheguei — iniciar viagem</button>`;
      if (v.status === 'EM_ANDAMENTO') acao = `<button class="btn btn-sucesso btn-sm" onclick="acaoMotorista(${v.id}, 'finalizar')">Finalizar viagem</button>`;
      if (v.status === 'CONCLUIDA') {
        acao = v.pagamento
          ? `<span class="badge badge-PAGO">Pago pelo cidadão (${esc(NOME_FORMA[v.pagamento.forma])})</span>`
          : '<span class="badge badge-PENDENTE">Aguardando pagamento do cidadão</span>';
      }
      return `
        <div class="item ${ATIVAS.includes(v.status) ? 'item-ativo' : ''}">
          <div class="item-topo">
            <span class="item-titulo">${esc(v.origem)} → ${esc(v.destino)}</span>
            <span class="badge badge-${v.status}">${NOME_STATUS_VIAGEM[v.status]}</span>
          </div>
          <div class="item-info">
            <span>👤 ${esc(v.cidadaoNome)}</span>
            <span>📅 ${dataHora(v.dataHora)}</span>
            <span>📏 ${String(v.distanciaKm).replace('.', ',')} km</span>
            <span>👶 Menores: ${v.qtdAcompanhantes}</span>
            ${v.status === 'CANCELADA' ? '' : infoValores(v)}
          </div>
          ${acao ? `<div class="item-acoes">${acao}</div>` : ''}
        </div>`;
    }).join('');
  } catch (e) {
    lista.innerHTML = `<p class="alerta alerta-erro">${esc(e.message)}</p>`;
  }
}

async function acaoMotorista(id, acao) {
  const textos = { aceitar: 'Viagem aceita!', iniciar: 'Viagem iniciada.', finalizar: 'Viagem concluída.' };
  try {
    await api('POST', `/viagens/${id}/${acao}`);
    toast(textos[acao]);
  } catch (e) {
    toast(e.message);
  }
  carregarAbertas();
  carregarCorridas();
}

/* =====================================================================
   ADMINISTRADOR
   ===================================================================== */
function mudarAba(id) {
  document.querySelectorAll('#menu .nav[data-aba]').forEach((t) => t.classList.toggle('ativo', t.dataset.aba === id));
  document.querySelectorAll('.tab-conteudo').forEach((c) => c.classList.toggle('hidden', c.id !== id));
  if (id === 'tab-pendentes') carregarPendentes();
  if (id === 'tab-usuarios') carregarUsuarios();
}

function resumoUsuario(u) {
  const partes = [
    `<span>CPF: ${esc(u.cpf)}</span>`,
    `<span>Idade: ${u.idade} anos</span>`,
    `<span>✉️ ${esc(u.email)}</span>`
  ];
  if (u.telefone) partes.push(`<span>📞 ${esc(u.telefone)}</span>`);
  if (u.tipo === 'CIDADAO') {
    partes.push(`<span>Renda familiar: <strong>${dinheiro(u.rendaFamiliar)}</strong></span>`);
    if (u.nis) partes.push(`<span>NIS: ${esc(u.nis)}</span>`);
  }
  if (u.tipo === 'MOTORISTA') {
    partes.push(`<span>CNH: ${esc(u.cnh)}</span>`);
    (u.veiculos || []).forEach((v) => partes.push(`<span>🚗 ${esc(v.marca)} ${esc(v.modelo)} ${v.ano} — ${esc(v.placa)}</span>`));
  }
  return partes.join('');
}

function botoesDocumentos(u) {
  if (!u.documentos || u.documentos.length === 0) return '<span class="subtexto">Nenhum documento enviado.</span>';
  return u.documentos.map((d) =>
    `<button class="btn btn-outline btn-sm" onclick="abrirDocumento(${d.id})" title="Enviado em ${dataHora(d.dataEnvio)}">📄 ${esc(NOME_DOC[d.tipo])}</button>`
  ).join('');
}

/** Indicadores do painel: pendentes (RF03) e o relatório do mês atual (RF14). */
async function carregarIndicadoresAdmin() {
  try {
    const hoje = new Date();
    const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    const r = await api('GET', `/relatorio?inicio=${iso(new Date(hoje.getFullYear(), hoje.getMonth(), 1))}&fim=${iso(hoje)}`);
    $('kpi-viagens-mes').textContent = r.quantidade;
    $('kpi-subsidio-mes').textContent = dinheiro(r.totalSubsidio);
  } catch (e) { /* indicadores são só informativos */ }
}

/** RF03: cadastros pendentes com documentos, aprovar e recusar. */
async function carregarPendentes() {
  const lista = $('lista-pendentes');
  try {
    const usuarios = await api('GET', '/usuarios?status=PENDENTE');
    $('kpi-pendentes').textContent = usuarios.length;
    if (usuarios.length === 0) {
      lista.innerHTML = '<p class="vazio">Nenhum cadastro pendente. 🎉</p>';
      return;
    }
    lista.innerHTML = usuarios.map((u) => `
      <div class="item">
        <div class="item-topo">
          <span class="item-titulo">${esc(u.nome)}</span>
          <span class="badge badge-${u.tipo}">${NOME_TIPO[u.tipo]}</span>
        </div>
        <div class="item-info">${resumoUsuario(u)}</div>
        <div class="item-acoes">${botoesDocumentos(u)}</div>
        <div class="item-acoes">
          <button class="btn btn-sucesso btn-sm" onclick="aprovarCadastro(${u.id})">✔ Aprovar</button>
          <button class="btn btn-perigo btn-sm" onclick="recusarCadastro(${u.id})">✖ Recusar</button>
        </div>
      </div>`).join('');
  } catch (e) {
    lista.innerHTML = `<p class="alerta alerta-erro">${esc(e.message)}</p>`;
  }
}

/** RNF02: só o administrador abre os documentos. */
async function abrirDocumento(id) {
  const janela = window.open('', '_blank');
  try {
    const resp = await fetch(`${API}/documentos/${id}`, { headers: { Authorization: `Bearer ${token}` } });
    if (!resp.ok) {
      const erro = await resp.json().catch(() => ({}));
      throw new Error(erro.erro || 'Não foi possível abrir o documento.');
    }
    const url = URL.createObjectURL(await resp.blob());
    if (janela) {
      janela.location.href = url;
    } else {
      const link = document.createElement('a');
      link.href = url;
      link.target = '_blank';
      link.click();
    }
  } catch (e) {
    if (janela) janela.close();
    toast(e.message);
  }
}

async function aprovarCadastro(id) {
  if (!await confirmar('Aprovar cadastro', 'Confirma que os documentos foram conferidos e o cadastro pode ser aprovado?', 'Aprovar', 'btn-sucesso')) return;
  try {
    await api('POST', `/usuarios/${id}/aprovar`);
    toast('Cadastro aprovado.');
  } catch (e) {
    toast(e.message);
  }
  carregarPendentes();
}

function recusarCadastro(id) {
  abrirModal('Recusar cadastro', `
    <form id="form-recusa" novalidate>
      <div class="campo">
        <label for="recusa-motivo">Motivo da recusa (o usuário verá este texto)</label>
        <input id="recusa-motivo" maxlength="255" placeholder="Ex.: comprovante de renda ilegível" />
      </div>
      <div id="recusa-msg" class="alerta hidden"></div>
      <div class="campo-acoes">
        <button type="button" class="btn btn-secondary" onclick="fecharModal()">Voltar</button>
        <button type="submit" class="btn btn-perigo">Recusar cadastro</button>
      </div>
    </form>`);
  $('form-recusa').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await api('POST', `/usuarios/${id}/recusar`, { motivo: $('recusa-motivo').value });
      fecharModal();
      toast('Cadastro recusado.');
      carregarPendentes();
    } catch (erro) {
      mensagem($('recusa-msg'), erro.message);
    }
  });
}

/** RF01: consulta de usuários pelo administrador. */
async function carregarUsuarios() {
  const lista = $('lista-usuarios');
  const tipo = $('filtro-tipo').value;
  const status = $('filtro-status').value;
  try {
    const usuarios = await api('GET', `/usuarios?tipo=${tipo}&status=${status}`);
    if (usuarios.length === 0) {
      lista.innerHTML = '<p class="vazio">Nenhum usuário encontrado.</p>';
      return;
    }
    lista.innerHTML = usuarios.map((u) => `
      <div class="item">
        <div class="item-topo">
          <span class="item-titulo">${esc(u.nome)}</span>
          <span>
            <span class="badge badge-${u.tipo}">${NOME_TIPO[u.tipo]}</span>
            <span class="badge badge-${u.status}">${NOME_STATUS_CADASTRO[u.status]}</span>
          </span>
        </div>
        <div class="item-info">${resumoUsuario(u)}</div>
        ${u.status === 'RECUSADO' ? `<div class="subtexto">Motivo da recusa: ${esc(u.motivoRecusa)}</div>` : ''}
        <div class="item-acoes">${botoesDocumentos(u)}</div>
      </div>`).join('');
  } catch (e) {
    lista.innerHTML = `<p class="alerta alerta-erro">${esc(e.message)}</p>`;
  }
}

/** RF14: relatório de viagens realizadas e subsídio pago no período. */
function prepararRelatorio() {
  const hoje = new Date();
  const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  if (!$('rel-inicio').value) $('rel-inicio').value = iso(new Date(hoje.getFullYear(), hoje.getMonth(), 1));
  if (!$('rel-fim').value) $('rel-fim').value = iso(hoje);
}

async function gerarRelatorio(evento) {
  evento.preventDefault();
  const msg = $('rel-msg');
  const saida = $('rel-resultado');
  mensagem(msg, null);
  try {
    const r = await api('GET', `/relatorio?inicio=${$('rel-inicio').value}&fim=${$('rel-fim').value}`);
    const linhas = r.viagens.map((v) => `
      <tr>
        <td>${dataHora(v.dataHora)}</td>
        <td>${esc(v.cidadaoNome)}</td>
        <td>${esc(v.motoristaNome)}</td>
        <td>${esc(v.origem)} → ${esc(v.destino)}</td>
        <td class="num">${String(v.distanciaKm).replace('.', ',')}</td>
        <td class="num">${dinheiro(v.valorTotal)}</td>
        <td class="num">${dinheiro(v.valorSubsidio)}</td>
        <td class="num">${dinheiro(v.valorCidadao)}</td>
      </tr>`).join('');
    saida.innerHTML = `
      <p class="subtexto">Período: ${dataCurta(r.inicio)} a ${dataCurta(r.fim)} — somente viagens concluídas
        (viagem cancelada não gera subsídio).</p>
      <div class="grid g-3" style="margin-top: 12px">
        <div class="card kpi"><div class="icone-kpi">🚐</div><div><small>Viagens realizadas</small><div class="num">${r.quantidade}</div></div></div>
        <div class="card kpi"><div class="icone-kpi">R$</div><div><small>Subsídio pago pela prefeitura</small><div class="num num-texto">${dinheiro(r.totalSubsidio)}</div></div></div>
        <div class="card kpi"><div class="icone-kpi">👥</div><div><small>Pago pelos cidadãos</small><div class="num num-texto">${dinheiro(r.totalCidadaos)}</div></div></div>
      </div>
      ${r.quantidade === 0 ? '<p class="vazio">Nenhuma viagem realizada neste período.</p>' : `
      <div class="tabela-wrap">
        <table class="tabela">
          <thead><tr>
            <th>Data</th><th>Cidadão</th><th>Motorista</th><th>Trajeto</th>
            <th class="num">Km</th><th class="num">Total</th><th class="num">Subsídio</th><th class="num">Cidadão</th>
          </tr></thead>
          <tbody>${linhas}</tbody>
          <tfoot><tr>
            <th colspan="5">Totais</th>
            <th class="num">${dinheiro(r.totalViagens)}</th>
            <th class="num">${dinheiro(r.totalSubsidio)}</th>
            <th class="num">${dinheiro(r.totalCidadaos)}</th>
          </tr></tfoot>
        </table>
      </div>`}`;
  } catch (e) {
    saida.innerHTML = '';
    mensagem(msg, e.message);
  }
}

/* =====================================================================
   MINHA CONTA — consultar, alterar e excluir (RF01)
   ===================================================================== */
async function abrirMinhaConta() {
  try {
    usuario = await api('GET', '/conta');
  } catch (e) {
    toast(e.message);
    return;
  }
  const u = usuario;
  let extras = '';
  if (u.tipo === 'CIDADAO') {
    extras = `<dt>Renda familiar</dt><dd>${dinheiro(u.rendaFamiliar)}</dd><dt>NIS</dt><dd>${esc(u.nis || '-')}</dd>`;
  } else if (u.tipo === 'MOTORISTA') {
    extras = `<dt>CNH</dt><dd>${esc(u.cnh)}</dd>`;
  }
  abrirModal('Minha conta', `
    <dl class="detalhes">
      <dt>Tipo</dt><dd>${NOME_TIPO[u.tipo]}</dd>
      <dt>CPF</dt><dd>${esc(u.cpf)}</dd>
      <dt>Nascimento</dt><dd>${dataCurta(u.dataNascimento)} (${u.idade} anos)</dd>
      <dt>Situação</dt><dd><span class="badge badge-${u.status}">${NOME_STATUS_CADASTRO[u.status]}</span></dd>
      ${extras}
    </dl>
    <form id="form-conta" novalidate>
      <div class="campo"><label for="conta-nome">Nome completo</label><input id="conta-nome" maxlength="100" value="${esc(u.nome)}" /></div>
      <div class="grid-2">
        <div class="campo"><label for="conta-email">E-mail</label><input id="conta-email" type="email" maxlength="100" value="${esc(u.email)}" /></div>
        <div class="campo"><label for="conta-telefone">Telefone</label><input id="conta-telefone" maxlength="20" value="${esc(u.telefone)}" /></div>
      </div>
      <div class="grid-2">
        <div class="campo"><label for="conta-senha-atual">Senha atual</label><input id="conta-senha-atual" type="password" placeholder="só para trocar a senha" /></div>
        <div class="campo"><label for="conta-senha-nova">Nova senha</label><input id="conta-senha-nova" type="password" placeholder="deixe vazio para manter" /></div>
      </div>
      <div id="conta-msg" class="alerta hidden"></div>
      <div class="campo-acoes">
        ${u.tipo !== 'ADMIN' ? '<button type="button" class="btn btn-perigo" id="btn-excluir-conta">Excluir minha conta</button>' : ''}
        <button type="submit" class="btn btn-primary">Salvar alterações</button>
      </div>
    </form>`);

  $('form-conta').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      usuario = await api('PUT', '/conta', {
        nome: $('conta-nome').value,
        email: $('conta-email').value,
        telefone: $('conta-telefone').value,
        senhaAtual: $('conta-senha-atual').value,
        senhaNova: $('conta-senha-nova').value
      });
      desenharCabecalho(usuario);
      fecharModal();
      toast('Dados atualizados.');
    } catch (erro) {
      mensagem($('conta-msg'), erro.message);
    }
  });

  const excluir = $('btn-excluir-conta');
  if (excluir) {
    excluir.addEventListener('click', async () => {
      if (!await confirmar('Excluir conta',
        'Tem certeza? Sua conta, seus documentos e seus veículos serão apagados. Essa ação não pode ser desfeita.',
        'Excluir definitivamente', 'btn-perigo')) return;
      try {
        await api('DELETE', '/conta');
        sairDoSistema();
        mensagem($('login-msg'), 'Sua conta foi excluída.', 'ok');
      } catch (erro) {
        toast(erro.message);
      }
    });
  }
}

/* =====================================================================
   Início
   ===================================================================== */
document.addEventListener('DOMContentLoaded', async () => {
  mascaraCpf($('login-cpf'));
  mascaraCpf($('cad-cpf'));

  $('form-login').addEventListener('submit', fazerLogin);
  $('form-cadastro').addEventListener('submit', cadastrar);
  $('link-cadastro').addEventListener('click', (e) => { e.preventDefault(); mensagem($('cad-msg'), null); mostrarTela('tela-cadastro'); });
  $('link-login').addEventListener('click', (e) => { e.preventDefault(); mostrarTela('tela-login'); });
  document.querySelectorAll('input[name="tipo-cad"]').forEach((r) => r.addEventListener('change', ajustarCadastro));
  ajustarCadastro();


  $('btn-calcular').addEventListener('click', calcularValor);
  $('form-solicitar').addEventListener('submit', solicitarViagem);
  $('sol-km').addEventListener('input', limparSimulacao);
  $('btn-atualizar-cidadao').addEventListener('click', carregarViagensCidadao);

  $('chk-disponivel').addEventListener('change', alterarDisponibilidade);
  $('btn-novo-veiculo').addEventListener('click', () => abrirVeiculo(0));
  $('btn-atualizar-abertas').addEventListener('click', carregarAbertas);
  $('btn-atualizar-motorista').addEventListener('click', carregarCorridas);

  $('btn-atualizar-pendentes').addEventListener('click', carregarPendentes);
  $('btn-filtrar').addEventListener('click', carregarUsuarios);
  $('form-relatorio').addEventListener('submit', gerarRelatorio);

  $('modal-fechar').addEventListener('click', fecharModal);
  $('modal-fundo').addEventListener('click', (e) => { if (e.target.id === 'modal-fundo') fecharModal(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') fecharModal(); });

  // Mantém a sessão ao recarregar a página
  token = recuperar('token');
  if (token) {
    try {
      entrarNoSistema(await api('GET', '/conta'));
    } catch (e) {
      token = null;
      guardar('token', null);
    }
  }
});
