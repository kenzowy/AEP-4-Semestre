/* ============================================================
   Transporte Social — Frontend (JavaScript Vanilla)
   Conecta ao backend Java via fetch() em http://localhost:8080
   ============================================================ */

'use strict';

const API = 'http://localhost:8080/api';

// Estado global do usuário logado
let usuarioLogado = null;

// ============================================================
// INICIALIZAÇÃO
// ============================================================

document.addEventListener('DOMContentLoaded', () => {
  // Formulário de login
  document.getElementById('form-login').addEventListener('submit', e => {
    e.preventDefault();
    fazerLogin();
  });

  // Formulário de cadastro
  document.getElementById('form-cadastro').addEventListener('submit', e => {
    e.preventDefault();
    fazerCadastro();
  });

  // Formulário de solicitar viagem
  document.getElementById('form-solicitar').addEventListener('submit', e => {
    e.preventDefault();
    solicitarViagem();
  });

  // Trocar campos conforme tipo de cadastro
  document.querySelectorAll('input[name="tipo-cad"]').forEach(r => {
    r.addEventListener('change', () => {
      const tipo = r.value;
      document.getElementById('campos-cidadao').classList.toggle('hidden', tipo !== 'CIDADAO');
      document.getElementById('campos-motorista').classList.toggle('hidden', tipo !== 'MOTORISTA');
    });
  });
});

// ============================================================
// NAVEGAÇÃO ENTRE TELAS
// ============================================================

function mostrarTela(id) {
  ['tela-login', 'tela-cadastro', 'tela-app'].forEach(t =>
    document.getElementById(t).classList.add('hidden')
  );
  document.getElementById(id).classList.remove('hidden');
}

function mostrarLogin() { mostrarTela('tela-login'); }
function mostrarCadastro() { mostrarTela('tela-cadastro'); }

// ============================================================
// LOGIN
// ============================================================

async function fazerLogin() {
  const email = document.getElementById('login-email').value.trim();
  const senha = document.getElementById('login-senha').value;
  const erroEl = document.getElementById('login-erro');
  erroEl.classList.add('hidden');

  try {
    const res = await fetch(`${API}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, senha })
    });
    const data = await res.json();

    if (!res.ok) {
      erroEl.textContent = data.erro || 'Credenciais inválidas.';
      erroEl.classList.remove('hidden');
      return;
    }

    if (data.status !== 'APROVADO' && data.tipo !== 'ADMINISTRADOR') {
      erroEl.textContent = 'Seu cadastro ainda não foi aprovado pelo administrador.';
      erroEl.classList.remove('hidden');
      return;
    }

    usuarioLogado = data;
    iniciarApp();
  } catch (err) {
    erroEl.textContent = 'Não foi possível conectar ao servidor. Verifique se o sistema está rodando.';
    erroEl.classList.remove('hidden');
  }
}

function fazerLogout() {
  usuarioLogado = null;
  mostrarTela('tela-login');
  document.getElementById('login-email').value = '';
  document.getElementById('login-senha').value = '';
}

// ============================================================
// CADASTRO
// ============================================================

async function fazerCadastro() {
  const tipo = document.querySelector('input[name="tipo-cad"]:checked').value;
  const erroEl = document.getElementById('cad-erro');
  const okEl   = document.getElementById('cad-ok');
  erroEl.classList.add('hidden');
  okEl.classList.add('hidden');

  const payload = {
    tipo,
    nome:          document.getElementById('cad-nome').value.trim(),
    cpf:           document.getElementById('cad-cpf').value.trim(),
    dataNascimento:document.getElementById('cad-nascimento').value,
    email:         document.getElementById('cad-email').value.trim(),
    telefone:      document.getElementById('cad-telefone').value.trim(),
    senha:         document.getElementById('cad-senha').value,
  };

  if (tipo === 'CIDADAO') {
    payload.rendaFamiliar = document.getElementById('cad-renda').value || '0';
    payload.nis           = document.getElementById('cad-nis').value.trim();
  } else {
    payload.cnh = document.getElementById('cad-cnh').value.trim();
  }

  try {
    const res = await fetch(`${API}/usuarios`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();

    if (!res.ok) {
      erroEl.textContent = data.erro || 'Erro ao cadastrar.';
      erroEl.classList.remove('hidden');
      return;
    }

    okEl.textContent = 'Cadastro realizado! Aguarde aprovação do administrador para acessar o sistema.';
    okEl.classList.remove('hidden');
    document.getElementById('form-cadastro').reset();
  } catch (err) {
    erroEl.textContent = 'Erro de conexão com o servidor.';
    erroEl.classList.remove('hidden');
  }
}

// ============================================================
// INICIAR APP (após login bem-sucedido)
// ============================================================

function iniciarApp() {
  mostrarTela('tela-app');

  // Navbar
  document.getElementById('nav-nome').textContent = usuarioLogado.nome;
  const badge = document.getElementById('nav-badge');
  badge.textContent = traduzirTipo(usuarioLogado.tipo);
  badge.className = 'badge badge-' + usuarioLogado.tipo.toLowerCase();

  // Esconder todos os painéis
  ['painel-cidadao','painel-motorista','painel-admin'].forEach(p =>
    document.getElementById(p).classList.add('hidden')
  );

  // Mostrar painel correto
  const tipo = usuarioLogado.tipo;
  if (tipo === 'CIDADAO') {
    document.getElementById('painel-cidadao').classList.remove('hidden');
    carregarLimiteSemanal();
    carregarViagensCidadao();
  } else if (tipo === 'MOTORISTA') {
    document.getElementById('painel-motorista').classList.remove('hidden');
    carregarDisponibilidade();
    carregarVeiculos();
    carregarSolicitacoes();
    carregarViagensMotorista();
  } else if (tipo === 'ADMINISTRADOR') {
    document.getElementById('painel-admin').classList.remove('hidden');
    carregarPendentes();
  }
}

// ============================================================
// CIDADÃO — Limite semanal
// ============================================================

async function carregarLimiteSemanal() {
  try {
    const res = await fetch(`${API}/viagens/limite?cidadaoId=${usuarioLogado.id}`);
    const data = await res.json();
    document.getElementById('stat-usadas').textContent   = data.usadas    ?? '-';
    document.getElementById('stat-restantes').textContent = data.restantes ?? '-';
  } catch { /* silencioso */ }
}

// ============================================================
// CIDADÃO — Calcular preview de valores
// ============================================================

function calcularPreview() {
  const km = parseFloat(document.getElementById('sol-km').value);
  if (!km || km <= 0) return;

  const TARIFA_BASE = 5.00;
  const VALOR_KM    = 2.00;
  const PCT_SUBSIDIO = 0.50;
  const TETO        = 15.00;

  const total    = TARIFA_BASE + (VALOR_KM * km);
  const subsidio = Math.min(total * PCT_SUBSIDIO, TETO);
  const paga     = Math.max(0, total - subsidio);

  document.getElementById('sol-total').textContent    = `R$ ${total.toFixed(2)}`;
  document.getElementById('sol-subsidio').textContent = `R$ ${subsidio.toFixed(2)}`;
  document.getElementById('sol-paga').textContent     = `R$ ${paga.toFixed(2)}`;
  document.getElementById('sol-preview').classList.remove('hidden');
}

// ============================================================
// CIDADÃO — Solicitar viagem
// ============================================================

async function solicitarViagem() {
  const erroEl = document.getElementById('sol-erro');
  erroEl.classList.add('hidden');

  const payload = {
    cidadaoId:  usuarioLogado.id,
    origem:     document.getElementById('sol-origem').value.trim(),
    destino:    document.getElementById('sol-destino').value.trim(),
    distanciaKm: document.getElementById('sol-km').value,
  };

  try {
    const res = await fetch(`${API}/viagens/solicitar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();

    if (!res.ok) {
      erroEl.textContent = data.erro || 'Erro ao solicitar viagem.';
      erroEl.classList.remove('hidden');
      return;
    }

    document.getElementById('form-solicitar').reset();
    document.getElementById('sol-preview').classList.add('hidden');
    carregarLimiteSemanal();
    carregarViagensCidadao();
    mostrarAlertaModal('Viagem solicitada!', `Viagem #${data.id} criada com sucesso. Aguarde um motorista aceitar.`);
  } catch (err) {
    erroEl.textContent = 'Erro de conexão.';
    erroEl.classList.remove('hidden');
  }
}

// ============================================================
// CIDADÃO — Listar minhas viagens
// ============================================================

async function carregarViagensCidadao() {
  const container = document.getElementById('lista-viagens-cidadao');
  container.innerHTML = '<p class="vazio">Carregando...</p>';
  try {
    const res = await fetch(`${API}/viagens?usuarioId=${usuarioLogado.id}`);
    const lista = await res.json();
    renderizarViagens(container, lista, 'cidadao');
  } catch {
    container.innerHTML = '<p class="vazio">Erro ao carregar viagens.</p>';
  }
}

// ============================================================
// CIDADÃO — Pagar viagem
// ============================================================

function abrirPagamento(viagemId, valorCidadao) {
  const corpo = `
    <div class="campo">
      <p style="margin-bottom:14px;">Valor a pagar: <strong>R$ ${parseFloat(valorCidadao).toFixed(2)}</strong></p>
      <label>Forma de pagamento</label>
      <select id="forma-pag">
        <option value="PIX">Pix</option>
        <option value="DEBITO">Débito</option>
        <option value="CREDITO">Crédito</option>
      </select>
    </div>
    <div class="modal-acoes">
      <button class="btn btn-secondary" onclick="fecharModal()">Cancelar</button>
      <button class="btn btn-primary" onclick="confirmarPagamento(${viagemId})">Confirmar pagamento</button>
    </div>
  `;
  abrirModal('Pagamento da viagem', corpo);
}

async function confirmarPagamento(viagemId) {
  const forma = document.getElementById('forma-pag').value;
  try {
    const res = await fetch(`${API}/pagamentos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ viagemId, formaPagamento: forma })
    });
    const data = await res.json();
    if (!res.ok) {
      alert(data.erro || 'Erro no pagamento.');
      return;
    }
    fecharModal();
    mostrarAlertaModal('Pagamento confirmado!',
      `Pagamento autorizado via ${forma}.\nValor pago: R$ ${parseFloat(data.valorCidadao).toFixed(2)}`);
    carregarViagensCidadao();
    carregarLimiteSemanal();
  } catch {
    alert('Erro de conexão.');
  }
}

// ============================================================
// MOTORISTA — Disponibilidade
// ============================================================

async function carregarDisponibilidade() {
  try {
    const res = await fetch(`${API}/usuarios/${usuarioLogado.id}`);
    const data = await res.json();
    const disp = data.disponivel === true;
    document.getElementById('chk-disponivel').checked = disp;
    document.getElementById('txt-disponivel').textContent =
      disp ? 'Você está disponível para receber corridas.' : 'Você está indisponível no momento.';
  } catch { /* silencioso */ }
}

async function alterarDisponibilidade(disponivel) {
  try {
    await fetch(`${API}/usuarios/disponibilidade`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: usuarioLogado.id, disponivel })
    });
    document.getElementById('txt-disponivel').textContent =
      disponivel ? 'Você está disponível para receber corridas.' : 'Você está indisponível no momento.';
  } catch { /* silencioso */ }
}

// ============================================================
// MOTORISTA — Veículos
// ============================================================

async function carregarVeiculos() {
  const container = document.getElementById('lista-veiculos');
  try {
    const res = await fetch(`${API}/veiculos?motoristaId=${usuarioLogado.id}`);
    const lista = await res.json();
    if (!lista.length) {
      container.innerHTML = '<p class="vazio">Nenhum veículo cadastrado.</p>';
      return;
    }
    container.innerHTML = lista.map(v => `
      <div class="veiculo-item">
        <div>
          <strong>${v.modelo}</strong>
          <span class="subtexto"> — ${v.cor || ''} · ${v.placa} · ${v.ano}</span>
        </div>
        <div style="display:flex;gap:6px;">
          <button class="btn btn-secondary btn-sm" onclick="editarVeiculo(${v.id},'${v.modelo}','${v.cor || ''}',${v.ano})">Editar</button>
          <button class="btn btn-perigo btn-sm" onclick="excluirVeiculo(${v.id})">Excluir</button>
        </div>
      </div>
    `).join('');
  } catch {
    container.innerHTML = '<p class="vazio">Erro ao carregar veículos.</p>';
  }
}

function abrirModalVeiculo(id, modelo, cor, ano) {
  const editando = !!id;
  const corpo = `
    <div class="campo"><label>Placa</label>
      <input id="v-placa" type="text" placeholder="AAA-0000" ${editando ? 'disabled' : ''} value="" />
    </div>
    <div class="campo"><label>Modelo</label>
      <input id="v-modelo" type="text" placeholder="Fiat Uno" value="${modelo || ''}" />
    </div>
    <div class="grid-2">
      <div class="campo"><label>Cor</label>
        <input id="v-cor" type="text" placeholder="Branco" value="${cor || ''}" />
      </div>
      <div class="campo"><label>Ano</label>
        <input id="v-ano" type="number" placeholder="2020" value="${ano || ''}" />
      </div>
    </div>
    <div class="modal-acoes">
      <button class="btn btn-secondary" onclick="fecharModal()">Cancelar</button>
      <button class="btn btn-primary" onclick="${editando ? `salvarVeiculo(${id})` : 'cadastrarVeiculo()'}">
        ${editando ? 'Salvar' : 'Cadastrar'}
      </button>
    </div>
  `;
  abrirModal(editando ? 'Editar veículo' : 'Adicionar veículo', corpo);
}

function editarVeiculo(id, modelo, cor, ano) {
  abrirModalVeiculo(id, modelo, cor, ano);
}

async function cadastrarVeiculo() {
  const payload = {
    motoristaId: usuarioLogado.id,
    placa:  document.getElementById('v-placa').value.trim(),
    modelo: document.getElementById('v-modelo').value.trim(),
    cor:    document.getElementById('v-cor').value.trim(),
    ano:    document.getElementById('v-ano').value,
  };
  try {
    const res = await fetch(`${API}/veiculos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) { alert(data.erro || 'Erro ao cadastrar.'); return; }
    fecharModal();
    carregarVeiculos();
  } catch { alert('Erro de conexão.'); }
}

async function salvarVeiculo(id) {
  const payload = {
    modelo: document.getElementById('v-modelo').value.trim(),
    cor:    document.getElementById('v-cor').value.trim(),
    ano:    document.getElementById('v-ano').value,
  };
  try {
    const res = await fetch(`${API}/veiculos/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (!res.ok) { alert('Erro ao salvar.'); return; }
    fecharModal();
    carregarVeiculos();
  } catch { alert('Erro de conexão.'); }
}

async function excluirVeiculo(id) {
  if (!confirm('Excluir este veículo?')) return;
  try {
    await fetch(`${API}/veiculos/${id}`, { method: 'DELETE' });
    carregarVeiculos();
  } catch { alert('Erro de conexão.'); }
}

// ============================================================
// MOTORISTA — Solicitações abertas
// ============================================================

async function carregarSolicitacoes() {
  const container = document.getElementById('lista-solicitacoes');
  container.innerHTML = '<p class="vazio">Carregando...</p>';
  try {
    const res = await fetch(`${API}/viagens?tipo=solicitacoes`);
    const lista = await res.json();
    renderizarViagens(container, lista, 'motorista-solicitacoes');
  } catch {
    container.innerHTML = '<p class="vazio">Erro ao carregar solicitações.</p>';
  }
}

async function aceitarViagem(viagemId) {
  try {
    const res = await fetch(`${API}/viagens/aceitar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ viagemId, motoristaId: usuarioLogado.id })
    });
    if (!res.ok) { alert('Erro ao aceitar.'); return; }
    document.getElementById('chk-disponivel').checked = false;
    document.getElementById('txt-disponivel').textContent = 'Você está indisponível no momento.';
    carregarSolicitacoes();
    carregarViagensMotorista();
  } catch { alert('Erro de conexão.'); }
}

// ============================================================
// MOTORISTA — Minhas corridas
// ============================================================

async function carregarViagensMotorista() {
  const container = document.getElementById('lista-viagens-motorista');
  container.innerHTML = '<p class="vazio">Carregando...</p>';
  try {
    const res = await fetch(`${API}/viagens?tipo=motorista&usuarioId=${usuarioLogado.id}`);
    const lista = await res.json();
    renderizarViagens(container, lista, 'motorista');
  } catch {
    container.innerHTML = '<p class="vazio">Erro ao carregar corridas.</p>';
  }
}

async function iniciarViagem(viagemId) {
  await alterarStatusViagem(viagemId, 'EM_ANDAMENTO');
  carregarViagensMotorista();
}

async function concluirViagem(viagemId) {
  await alterarStatusViagem(viagemId, 'CONCLUIDA');
  carregarDisponibilidade();
  carregarViagensMotorista();
}

async function alterarStatusViagem(viagemId, status) {
  try {
    const res = await fetch(`${API}/viagens/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ viagemId, status })
    });
    if (!res.ok) alert('Erro ao atualizar status.');
  } catch { alert('Erro de conexão.'); }
}

// ============================================================
// ADMINISTRADOR — Pendentes
// ============================================================

async function carregarPendentes() {
  const container = document.getElementById('lista-pendentes');
  container.innerHTML = '<p class="vazio">Carregando...</p>';
  try {
    const res = await fetch(`${API}/usuarios?status=PENDENTE`);
    const lista = await res.json();
    if (!lista.length) {
      container.innerHTML = '<p class="vazio">Nenhum cadastro pendente. 🎉</p>';
      return;
    }
    container.innerHTML = lista.map(u => `
      <div class="usuario-item">
        <div class="usuario-info">
          <div class="usuario-nome">${escHtml(u.nome)}
            <span class="badge badge-${u.tipo.toLowerCase()}">${traduzirTipo(u.tipo)}</span>
          </div>
          <div class="usuario-detalhe">
            ${escHtml(u.email)} · CPF: ${escHtml(u.cpf || '-')}
            ${u.tipo === 'CIDADAO' ? ` · Renda: R$ ${parseFloat(u.rendaFamiliar || 0).toFixed(2)}` : ''}
            ${u.tipo === 'MOTORISTA' ? ` · CNH: ${escHtml(u.cnh || '-')}` : ''}
          </div>
        </div>
        <div class="usuario-acoes">
          <button class="btn btn-sucesso btn-sm" onclick="aprovarUsuario(${u.id}, true)">Aprovar</button>
          <button class="btn btn-perigo btn-sm" onclick="recusarUsuario(${u.id})">Recusar</button>
        </div>
      </div>
    `).join('');
  } catch {
    container.innerHTML = '<p class="vazio">Erro ao carregar pendentes.</p>';
  }
}

async function aprovarUsuario(id, aprovado, motivo) {
  try {
    const res = await fetch(`${API}/usuarios/aprovar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id, aprovado, motivo: motivo || '' })
    });
    if (!res.ok) { alert('Erro ao processar.'); return; }
    fecharModal();
    carregarPendentes();
  } catch { alert('Erro de conexão.'); }
}

function recusarUsuario(id) {
  const corpo = `
    <div class="campo">
      <label>Motivo da recusa</label>
      <input id="motivo-recusa" type="text" placeholder="Informe o motivo..." />
    </div>
    <div class="modal-acoes">
      <button class="btn btn-secondary" onclick="fecharModal()">Cancelar</button>
      <button class="btn btn-perigo" onclick="confirmarRecusa(${id})">Confirmar recusa</button>
    </div>
  `;
  abrirModal('Recusar cadastro', corpo);
}

async function confirmarRecusa(id) {
  const motivo = document.getElementById('motivo-recusa').value.trim();
  await aprovarUsuario(id, false, motivo);
}

// ============================================================
// ADMINISTRADOR — Cidadãos / Motoristas
// ============================================================

async function carregarCidadaos() {
  const container = document.getElementById('lista-cidadaos');
  container.innerHTML = '<p class="vazio">Carregando...</p>';
  try {
    const res = await fetch(`${API}/usuarios?tipo=CIDADAO&status=APROVADO`);
    const lista = await res.json();
    renderizarUsuariosAdmin(container, lista);
  } catch {
    container.innerHTML = '<p class="vazio">Erro ao carregar.</p>';
  }
}

async function carregarMotoristas() {
  const container = document.getElementById('lista-motoristas');
  container.innerHTML = '<p class="vazio">Carregando...</p>';
  try {
    const res = await fetch(`${API}/usuarios?tipo=MOTORISTA&status=APROVADO`);
    const lista = await res.json();
    renderizarUsuariosAdmin(container, lista);
  } catch {
    container.innerHTML = '<p class="vazio">Erro ao carregar.</p>';
  }
}

function renderizarUsuariosAdmin(container, lista) {
  if (!lista.length) { container.innerHTML = '<p class="vazio">Nenhum usuário encontrado.</p>'; return; }
  container.innerHTML = lista.map(u => `
    <div class="usuario-item">
      <div class="usuario-info">
        <div class="usuario-nome">${escHtml(u.nome)}</div>
        <div class="usuario-detalhe">
          ${escHtml(u.email)} · CPF: ${escHtml(u.cpf || '-')}
          ${u.tipo === 'MOTORISTA' ? ` · Disponível: ${u.disponivel ? 'Sim' : 'Não'}` : ''}
        </div>
      </div>
      <div class="usuario-acoes">
        <button class="btn btn-perigo btn-sm" onclick="excluirUsuario(${u.id})">Excluir</button>
      </div>
    </div>
  `).join('');
}

async function excluirUsuario(id) {
  if (!confirm('Excluir este usuário? Esta ação não pode ser desfeita.')) return;
  try {
    const res = await fetch(`${API}/usuarios/${id}`, { method: 'DELETE' });
    if (!res.ok) { alert('Erro ao excluir.'); return; }
    carregarCidadaos();
    carregarMotoristas();
  } catch { alert('Erro de conexão.'); }
}

// ============================================================
// ADMINISTRADOR — Viagens
// ============================================================

async function carregarViagensAdmin() {
  const container = document.getElementById('lista-viagens-admin');
  container.innerHTML = '<p class="vazio">Carregando...</p>';
  try {
    const res = await fetch(`${API}/viagens?tipo=solicitacoes`);
    const lista = await res.json();
    renderizarViagens(container, lista, 'admin');
  } catch {
    container.innerHTML = '<p class="vazio">Erro ao carregar.</p>';
  }
}

// ============================================================
// RENDERIZADORES
// ============================================================

function renderizarViagens(container, lista, contexto) {
  if (!lista.length) {
    container.innerHTML = '<p class="vazio">Nenhuma viagem encontrada.</p>';
    return;
  }

  container.innerHTML = lista.map(v => {
    const statusBadge = `<span class="badge badge-${v.status.toLowerCase()}">${traduzirStatus(v.status)}</span>`;
    const valores = v.valorCidadao !== undefined
      ? `<span>Você paga: <strong>R$ ${parseFloat(v.valorCidadao).toFixed(2)}</strong></span>
         <span>Subsídio: R$ ${parseFloat(v.valorSubsidio || 0).toFixed(2)}</span>`
      : '';

    let acoes = '';

    if (contexto === 'cidadao') {
      if (v.status === 'SOLICITADA') {
        acoes = `<button class="btn btn-perigo btn-sm" onclick="cancelarViagem(${v.id})">Cancelar</button>`;
      } else if (v.status === 'CONCLUIDA' && !v.pago) {
        acoes = `<button class="btn btn-primary btn-sm" onclick="abrirPagamento(${v.id}, '${v.valorCidadao}')">Pagar</button>`;
      }
    } else if (contexto === 'motorista-solicitacoes') {
      acoes = `<button class="btn btn-sucesso btn-sm" onclick="aceitarViagem(${v.id})">Aceitar corrida</button>`;
    } else if (contexto === 'motorista') {
      if (v.status === 'ACEITA') {
        acoes = `<button class="btn btn-primary btn-sm" onclick="iniciarViagem(${v.id})">Iniciar</button>`;
      } else if (v.status === 'EM_ANDAMENTO') {
        acoes = `<button class="btn btn-sucesso btn-sm" onclick="concluirViagem(${v.id})">Concluir</button>`;
      }
    }

    const motorista = v.nomeMotorista ? `<span>Motorista: ${escHtml(v.nomeMotorista)}</span>` : '<span>Sem motorista</span>';
    const cidadao   = v.nomeCidadao   ? `<span>Cidadão: ${escHtml(v.nomeCidadao)}</span>` : '';
    const tempo     = v.tempoEstimado ? `<span>⏱ ${v.tempoEstimado} min</span>` : '';
    const dist      = v.distanciaKm   ? `<span>📍 ${v.distanciaKm} km</span>` : '';
    const data      = v.dataHora      ? `<span>${new Date(v.dataHora).toLocaleString('pt-BR')}</span>` : '';

    return `
      <div class="viagem-item">
        <div class="viagem-rota">
          🏠 ${escHtml(v.origem)} → 🎯 ${escHtml(v.destino)}
          ${statusBadge}
        </div>
        <div class="viagem-info">
          ${cidadao}${motorista}${dist}${tempo}${valores}${data}
        </div>
        ${acoes ? `<div class="viagem-acoes">${acoes}</div>` : ''}
      </div>
    `;
  }).join('');
}

// ============================================================
// CANCELAR VIAGEM
// ============================================================

async function cancelarViagem(viagemId) {
  if (!confirm('Cancelar esta viagem?')) return;
  try {
    const res = await fetch(`${API}/viagens/cancelar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ viagemId })
    });
    if (!res.ok) { const d = await res.json(); alert(d.erro || 'Não foi possível cancelar.'); return; }
    carregarViagensCidadao();
    carregarLimiteSemanal();
  } catch { alert('Erro de conexão.'); }
}

// ============================================================
// TABS (admin)
// ============================================================

function mudarTab(id, btn) {
  document.querySelectorAll('.tab-conteudo').forEach(t => t.classList.add('hidden'));
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('ativo'));
  document.getElementById(id).classList.remove('hidden');
  btn.classList.add('ativo');

  // Carregar dados ao trocar aba
  if (id === 'tab-pendentes')     carregarPendentes();
  if (id === 'tab-cidadaos')      carregarCidadaos();
  if (id === 'tab-motoristas')    carregarMotoristas();
  if (id === 'tab-viagens-admin') carregarViagensAdmin();
}

// ============================================================
// MODAL
// ============================================================

function abrirModal(titulo, corpo) {
  document.getElementById('modal-titulo').textContent = titulo;
  document.getElementById('modal-corpo').innerHTML = corpo;
  document.getElementById('modal-backdrop').classList.remove('hidden');
}

function fecharModal() {
  document.getElementById('modal-backdrop').classList.add('hidden');
}

function mostrarAlertaModal(titulo, mensagem) {
  abrirModal(titulo, `
    <p>${escHtml(mensagem)}</p>
    <div class="modal-acoes">
      <button class="btn btn-primary" onclick="fecharModal()">OK</button>
    </div>
  `);
}

// ============================================================
// UTILITÁRIOS
// ============================================================

function traduzirTipo(tipo) {
  return { CIDADAO: 'Cidadão', MOTORISTA: 'Motorista', ADMINISTRADOR: 'Admin' }[tipo] || tipo;
}

function traduzirStatus(status) {
  return {
    SOLICITADA:   'Solicitada',
    ACEITA:       'Aceita',
    EM_ANDAMENTO: 'Em andamento',
    CONCLUIDA:    'Concluída',
    CANCELADA:    'Cancelada',
  }[status] || status;
}

function escHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
