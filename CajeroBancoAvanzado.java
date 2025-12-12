import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * CajeroBancoAvanzado.java
 *
 * - Dos usuarios (personas) separados.
 * - Cada persona tiene 2 "tarjetas"/cuentas: Corriente y Ahorros.
 * - Saldo inicial S/ 2000.00 en cada subcuenta.
 * - Login por DNI + contraseña.
 * - Seleccionar cuál de TUS cuentas operar.
 * - Mostrar/ocultar saldo (por defecto ****).
 * - Transferencia entre tus propias cuentas (interna).
 * - Transferencia a cuenta de otra persona (requiere código).
 * - Depositar efectivo a TU cuenta seleccionada.
 * - Ver ambos saldos propios (requiere código).
 * - Interfaz de depósito con animación de carga.
 *
 * Cuentas demo:
 *  - Persona 1: DNI 12345678 / pass1 -> 1001-C (Corriente), 1001-A (Ahorros)
 *  - Persona 2: DNI 87654321 / pass2 -> 2002-C (Corriente), 2002-A (Ahorros)
 *
 * Código secreto (transferencias externas / ver ambos saldos): "1234"
 */
public class CajeroBancoAvanzado extends JFrame {

    // --- Modelo de datos ---
    static class SubCuenta {
        String tipo; // "Corriente" o "Ahorros"
        String numero; // ej "1001-C"
        double saldo;

        SubCuenta(String tipo, String numero, double saldo) {
            this.tipo = tipo;
            this.numero = numero;
            this.saldo = saldo;
        }

        @Override
        public String toString() {
            return tipo + " (" + numero + ")";
        }
    }

    static class Persona {
        String nombre;
        String dni;
        String password;
        Map<String, SubCuenta> cuentas = new LinkedHashMap<>(); // key: "Corriente"/"Ahorros"

        Persona(String nombre, String dni, String password) {
            this.nombre = nombre;
            this.dni = dni;
            this.password = password;
        }

        void agregarCuenta(SubCuenta sc) {
            cuentas.put(sc.tipo, sc);
        }
    }

    private final Map<String, Persona> personasPorDni = new HashMap<>();
    private Persona personaActiva = null;
    private SubCuenta cuentaSeleccionada = null;
    private final String CODIGO_SECRET0 = "1234";
    private final DecimalFormat df = new DecimalFormat("###,##0.00");

    // --- Componentes GUI ---
    private CardLayout cardLayout = new CardLayout();
    private JPanel cards;

    // Login
    private JTextField tfDni;
    private JPasswordField pfPass;

    // Main
    private JLabel lblBienvenida;
    private JComboBox<SubCuenta> cbMisCuentas;
    private JLabel lblCuentaNumero;
    private JLabel lblSaldo;
    private boolean saldoVisible = false;

    // Operaciones
    private JTextField tfMontoInterno;
    private JTextField tfMontoToOther;
    private JTextField tfDestinoCuenta; // número destino para transferir a otro usuario
    private JTextField tfMontoDepositoEfectivo;
    private JLabel lblMensaje;

    // Pantalla de depósito
    private JLabel lblEstadoDeposito;
    private JProgressBar progressBar;
    private javax.swing.Timer timerDeposito;
    private double montoDeposito;

    public CajeroBancoAvanzado() {
        setTitle("Banco Iberoamericano - Cajero Avanzado");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        crearPersonasYCuentasDemo();
        initGUI();
    }

    private void crearPersonasYCuentasDemo() {
        // Persona 1
        Persona p1 = new Persona("Kevin", "12345678", "pass1");
        p1.agregarCuenta(new SubCuenta("Corriente", "1001-C", 2000.00));
        p1.agregarCuenta(new SubCuenta("Ahorros", "1001-A", 2000.00));
        personasPorDni.put(p1.dni, p1);

        // Persona 2
        Persona p2 = new Persona("María", "87654321", "pass2");
        p2.agregarCuenta(new SubCuenta("Corriente", "2002-C", 2000.00));
        p2.agregarCuenta(new SubCuenta("Ahorros", "2002-A", 2000.00));
        personasPorDni.put(p2.dni, p2);
    }

    private void initGUI() {
        cards = new JPanel(cardLayout);
        cards.add(buildLoginPanel(), "LOGIN");
        cards.add(buildMainPanel(), "MAIN");
        cards.add(buildDepositoPanel(), "DEPOSITO");
        add(cards);
        cardLayout.show(cards, "LOGIN");
    }

    private JPanel buildLoginPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(224, 242, 255));
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblBank = new JLabel("BANCO IBEROAMERICANO", SwingConstants.CENTER);
        lblBank.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblBank.setForeground(new Color(6, 66, 120));
        p.add(lblBank, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(20, 100, 20, 100));

        JLabel welcome = new JLabel("<html><div style='text-align:center'>Bienvenido al cajero virtual<br><small>Ingrese DNI y contraseña</small></div></html>", SwingConstants.CENTER);
        welcome.setFont(new Font("SansSerif", Font.PLAIN, 14));
        center.add(welcome);
        center.add(Box.createVerticalStrut(20));

        tfDni = new JTextField();
        tfDni.setPreferredSize(new Dimension(200, 30));
        pfPass = new JPasswordField();
        pfPass.setPreferredSize(new Dimension(200, 30));
        
        center.add(new JLabel("DNI:"));
        center.add(Box.createVerticalStrut(5));
        center.add(tfDni);
        center.add(Box.createVerticalStrut(10));
        center.add(new JLabel("Contraseña:"));
        center.add(Box.createVerticalStrut(5));
        center.add(pfPass);
        center.add(Box.createVerticalStrut(20));

        JPanel btns = new JPanel(new FlowLayout());
        btns.setOpaque(false);
        JButton btnIngresar = new JButton("Ingresar");
        btnIngresar.setPreferredSize(new Dimension(100, 35));
        btnIngresar.addActionListener(e -> intentarLogin());
        JButton btnSalir = new JButton("Salir");
        btnSalir.setPreferredSize(new Dimension(100, 35));
        btnSalir.addActionListener(e -> System.exit(0));
        btns.add(btnIngresar);
        btns.add(Box.createHorizontalStrut(10));
        btns.add(btnSalir);
        center.add(btns);

        center.add(Box.createVerticalStrut(20));
        JLabel nota = new JLabel("<html><div style='text-align:center'><small>Cuentas demo:<br>Persona 1: DNI 12345678 / pass1 (1001-C, 1001-A)<br>Persona 2: DNI 87654321 / pass2 (2002-C, 2002-A)</small></div></html>", SwingConstants.CENTER);
        nota.setFont(new Font("SansSerif", Font.ITALIC, 11));
        center.add(nota);

        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildMainPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(245, 254, 255));
        p.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 15, 0));
        lblBienvenida = new JLabel("Usuario: -", SwingConstants.LEFT);
        lblBienvenida.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblBienvenida.setForeground(new Color(6, 66, 120));
        top.add(lblBienvenida, BorderLayout.WEST);
        JButton btnLogout = new JButton("Cerrar sesión");
        btnLogout.setPreferredSize(new Dimension(120, 30));
        btnLogout.addActionListener(e -> logout());
        top.add(btnLogout, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        // Center: tarjeta principal
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(10, 30, 10, 30));

        // Sección de cuenta
        JPanel cuentaPanel = new JPanel();
        cuentaPanel.setOpaque(false);
        cuentaPanel.setLayout(new BoxLayout(cuentaPanel, BoxLayout.Y_AXIS));
        cuentaPanel.setBorder(BorderFactory.createTitledBorder("Mi Cuenta"));
        
        cuentaPanel.add(new JLabel("Selecciona tu cuenta:"));
        cuentaPanel.add(Box.createVerticalStrut(5));
        cbMisCuentas = new JComboBox<>();
        cbMisCuentas.setPreferredSize(new Dimension(300, 30));
        cbMisCuentas.addActionListener(e -> cambiarCuentaSeleccionada());
        cuentaPanel.add(cbMisCuentas);

        cuentaPanel.add(Box.createVerticalStrut(15));
        lblCuentaNumero = new JLabel("Cuenta: -");
        lblCuentaNumero.setFont(new Font("Monospaced", Font.BOLD, 14));
        cuentaPanel.add(lblCuentaNumero);

        lblSaldo = new JLabel("Saldo: ****");
        lblSaldo.setFont(new Font("Monospaced", Font.BOLD, 22));
        lblSaldo.setBorder(new EmptyBorder(8, 0, 8, 0));
        cuentaPanel.add(lblSaldo);

        JPanel saldoBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        saldoBtns.setOpaque(false);
        JButton btnToggle = new JButton("Ver/ocultar saldo");
        btnToggle.setPreferredSize(new Dimension(150, 30));
        btnToggle.addActionListener(e -> toggleSaldo());
        saldoBtns.add(btnToggle);

        JButton btnVerMisDos = new JButton("Ver mis 2 saldos (requiere código)");
        btnVerMisDos.setPreferredSize(new Dimension(250, 30));
        btnVerMisDos.addActionListener(e -> pedirCodigoYMostrarMisDosSaldos());
        saldoBtns.add(btnVerMisDos);

        cuentaPanel.add(saldoBtns);
        center.add(cuentaPanel);

        center.add(Box.createVerticalStrut(20));
        center.add(buildPanelInternoTransferencia());
        center.add(Box.createVerticalStrut(15));
        center.add(buildPanelTransferirAotroUsuario());
        center.add(Box.createVerticalStrut(15));
        center.add(buildPanelDepositarEfectivo());
        center.add(Box.createVerticalStrut(20));

        lblMensaje = new JLabel(" ");
        lblMensaje.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblMensaje.setForeground(new Color(0, 120, 0));
        center.add(lblMensaje);

        scrollPane.setViewportView(center);
        p.add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));
        JLabel foot = new JLabel("Banco Iberoamericano ©", SwingConstants.CENTER);
        footer.add(foot, BorderLayout.CENTER);
        p.add(footer, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildPanelInternoTransferencia() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Transferencia entre tus cuentas"));
        
        JPanel contenido = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contenido.setOpaque(false);
        contenido.add(new JLabel("Monto S/"));
        tfMontoInterno = new JTextField(10);
        tfMontoInterno.setPreferredSize(new Dimension(100, 25));
        contenido.add(tfMontoInterno);
        JButton btnTransferirInterno = new JButton("Transferir entre mis cuentas");
        btnTransferirInterno.setPreferredSize(new Dimension(200, 30));
        btnTransferirInterno.addActionListener(e -> transferirEntreMisCuentas());
        contenido.add(btnTransferirInterno);
        
        panel.add(contenido);
        return panel;
    }

    private JPanel buildPanelTransferirAotroUsuario() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Transferir a otra persona (requiere código)"));
        
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila1.setOpaque(false);
        fila1.add(new JLabel("Cuenta destino:"));
        tfDestinoCuenta = new JTextField(12);
        tfDestinoCuenta.setPreferredSize(new Dimension(120, 25));
        fila1.add(tfDestinoCuenta);
        fila1.add(new JLabel("Monto S/"));
        tfMontoToOther = new JTextField(10);
        tfMontoToOther.setPreferredSize(new Dimension(100, 25));
        fila1.add(tfMontoToOther);
        
        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila2.setOpaque(false);
        JButton btnTransferirOtro = new JButton("Transferir (requiere código)");
        btnTransferirOtro.setPreferredSize(new Dimension(200, 30));
        btnTransferirOtro.addActionListener(e -> transferirAOtraPersonaConCodigo());
        fila2.add(btnTransferirOtro);
        
        panel.add(fila1);
        panel.add(fila2);
        return panel;
    }

    private JPanel buildPanelDepositarEfectivo() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Depositar efectivo a mi cuenta"));
        
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila1.setOpaque(false);
        fila1.add(new JLabel("Monto a depositar S/"));
        tfMontoDepositoEfectivo = new JTextField(10);
        tfMontoDepositoEfectivo.setPreferredSize(new Dimension(100, 25));
        fila1.add(tfMontoDepositoEfectivo);
        
        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila2.setOpaque(false);
        JButton btnDepositar = new JButton("Depositar efectivo");
        btnDepositar.setPreferredSize(new Dimension(150, 30));
        btnDepositar.addActionListener(e -> iniciarProcesoDeposito());
        fila2.add(btnDepositar);
        
        panel.add(fila1);
        panel.add(fila2);
        return panel;
    }

    private JPanel buildDepositoPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(240, 248, 255));
        p.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header
        JLabel header = new JLabel("PROCESANDO DEPÓSITO", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 24));
        header.setForeground(new Color(6, 66, 120));
        header.setBorder(new EmptyBorder(0, 0, 30, 0));
        p.add(header, BorderLayout.NORTH);

        // Centro - Estado del depósito
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(20, 50, 20, 50));

        lblEstadoDeposito = new JLabel("Iniciando depósito...", SwingConstants.CENTER);
        lblEstadoDeposito.setFont(new Font("SansSerif", Font.PLAIN, 18));
        lblEstadoDeposito.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(lblEstadoDeposito);

        center.add(Box.createVerticalStrut(30));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setPreferredSize(new Dimension(400, 25));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(progressBar);

        center.add(Box.createVerticalStrut(40));

        // Panel de botones (inicialmente oculto)
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setOpaque(false);
        btnPanel.setName("btnPanel");
        btnPanel.setVisible(false);

        JButton btnVerSaldo = new JButton("Ver Saldo");
        btnVerSaldo.setPreferredSize(new Dimension(100, 35));
        btnVerSaldo.addActionListener(e -> mostrarSaldoActual());
        btnPanel.add(btnVerSaldo);

        btnPanel.add(Box.createHorizontalStrut(10));

        JButton btnRetroceder = new JButton("Retroceder");
        btnRetroceder.setPreferredSize(new Dimension(100, 35));
        btnRetroceder.addActionListener(e -> volverAlMain());
        btnPanel.add(btnRetroceder);

        btnPanel.add(Box.createHorizontalStrut(10));

        JButton btnCerrarSesion = new JButton("Cerrar Sesión");
        btnCerrarSesion.setPreferredSize(new Dimension(120, 35));
        btnCerrarSesion.addActionListener(e -> logout());
        btnPanel.add(btnCerrarSesion);

        center.add(btnPanel);

        p.add(center, BorderLayout.CENTER);

        return p;
    }

    // --- Acciones ---

    private void intentarLogin() {
        String dni = tfDni.getText().trim();
        String pass = new String(pfPass.getPassword());

        if (dni.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese DNI y contraseña.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Persona p = personasPorDni.get(dni);
        if (p == null || !p.password.equals(pass)) {
            JOptionPane.showMessageDialog(this, "DNI o contraseña incorrectos.", "Error de autenticación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        personaActiva = p;
        configurarCuentasEnCombo();
        lblBienvenida.setText("Usuario: " + personaActiva.nombre + " (DNI " + personaActiva.dni + ")");
        lblMensaje.setText("Sesión iniciada. Selecciona una de tus cuentas para operar.");
        tfDni.setText("");
        pfPass.setText("");
        cardLayout.show(cards, "MAIN");
    }

    private void configurarCuentasEnCombo() {
        cbMisCuentas.removeAllItems();
        for (SubCuenta sc : personaActiva.cuentas.values()) {
            cbMisCuentas.addItem(sc);
        }
        // seleccionar primera por defecto
        if (cbMisCuentas.getItemCount() > 0) {
            cbMisCuentas.setSelectedIndex(0);
            cuentaSeleccionada = (SubCuenta) cbMisCuentas.getSelectedItem();
            actualizarInfoCuentaSeleccionada();
        }
    }

    private void cambiarCuentaSeleccionada() {
        cuentaSeleccionada = (SubCuenta) cbMisCuentas.getSelectedItem();
        saldoVisible = false;
        actualizarInfoCuentaSeleccionada();
    }

    private void actualizarInfoCuentaSeleccionada() {
        if (cuentaSeleccionada == null) {
            lblCuentaNumero.setText("Cuenta: -");
            lblSaldo.setText("Saldo: ****");
            return;
        }
        lblCuentaNumero.setText("Cuenta seleccionada: " + cuentaSeleccionada.tipo + " - " + cuentaSeleccionada.numero);
        actualizarSaldoLabel();
    }

    private void toggleSaldo() {
        if (cuentaSeleccionada == null) return;
        saldoVisible = !saldoVisible;
        actualizarSaldoLabel();
    }

    private void actualizarSaldoLabel() {
        if (cuentaSeleccionada == null) {
            lblSaldo.setText("Saldo: ****");
            return;
        }
        if (saldoVisible) {
            lblSaldo.setText("Saldo: S/ " + df.format(cuentaSeleccionada.saldo));
        } else {
            lblSaldo.setText("Saldo: ****");
        }
    }

    private void transferirEntreMisCuentas() {
        if (personaActiva == null || cuentaSeleccionada == null) return;
        String texto = tfMontoInterno.getText().trim();
        double monto;
        try {
            monto = Double.parseDouble(texto);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un monto válido.", "Monto inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (monto <= 0) {
            JOptionPane.showMessageDialog(this, "El monto debe ser mayor que 0.", "Monto inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // identificar la otra subcuenta del usuario
        SubCuenta destino = personaActiva.cuentas.values().stream().filter(sc -> sc != cuentaSeleccionada).findFirst().orElse(null);
        if (destino == null) {
            JOptionPane.showMessageDialog(this, "No se encontró otra cuenta suya.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (cuentaSeleccionada.saldo < monto) {
            JOptionPane.showMessageDialog(this, "Fondos insuficientes en la cuenta origen.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Confirmación
        int opt = JOptionPane.showConfirmDialog(this,
                String.format("Confirma transferencia S/ %s de %s → %s ?", df.format(monto), cuentaSeleccionada.numero, destino.numero),
                "Confirmar transferencia", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) return;

        cuentaSeleccionada.saldo -= monto;
        destino.saldo += monto;
        tfMontoInterno.setText("");
        lblMensaje.setText("Transferencia interna realizada a " + destino.numero + ".");
        actualizarSaldoLabel();
    }

    private void transferirAOtraPersonaConCodigo() {
        if (personaActiva == null || cuentaSeleccionada == null) return;
        String destNum = tfDestinoCuenta.getText().trim();
        String montoText = tfMontoToOther.getText().trim();
        if (destNum.isEmpty() || montoText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese cuenta destino y monto.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double monto;
        try {
            monto = Double.parseDouble(montoText);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un monto válido.", "Monto inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (monto <= 0) {
            JOptionPane.showMessageDialog(this, "El monto debe ser mayor que 0.", "Monto inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // pedir código
        String codigo = JOptionPane.showInputDialog(this, "Ingrese el código de autorización:", "Código requerido", JOptionPane.PLAIN_MESSAGE);
        if (codigo == null) return;
        if (!codigo.equals(CODIGO_SECRET0)) {
            JOptionPane.showMessageDialog(this, "Código incorrecto. Operación cancelada.", "Código inválido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // buscar cuenta destino entre todas las personas
        SubCuenta destino = buscarSubCuentaPorNumero(destNum);
        if (destino == null) {
            JOptionPane.showMessageDialog(this, "Cuenta destino no encontrada.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (cuentaSeleccionada.saldo < monto) {
            JOptionPane.showMessageDialog(this, "Fondos insuficientes en la cuenta origen.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // realizar transferencia (debitas tu cuenta)
        cuentaSeleccionada.saldo -= monto;
        destino.saldo += monto;
        tfDestinoCuenta.setText("");
        tfMontoToOther.setText("");
        lblMensaje.setText("Transferencia externa realizada a " + destino.numero + ".");
        actualizarSaldoLabel();
    }

    private void iniciarProcesoDeposito() {
        if (personaActiva == null || cuentaSeleccionada == null) return;
        
        String montoText = tfMontoDepositoEfectivo.getText().trim();
        if (montoText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese el monto de depósito.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        double monto;
        try {
            monto = Double.parseDouble(montoText);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un monto válido.", "Monto inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (monto <= 0) {
            JOptionPane.showMessageDialog(this, "El monto debe ser mayor que 0.", "Monto inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Guardar datos del depósito
        this.montoDeposito = monto;
        
        // Cambiar a pantalla de depósito
        cardLayout.show(cards, "DEPOSITO");
        
        // Iniciar animación de depósito
        iniciarAnimacionDeposito();
    }

    private void iniciarAnimacionDeposito() {
        progressBar.setValue(0);
        progressBar.setString("0%");
        lblEstadoDeposito.setText("Validando datos...");
        
        // Ocultar botones
        Component btnPanel = null;
        for (Component c : ((JPanel)cards.getComponent(2)).getComponents()) {
            if (c instanceof JPanel) {
                for (Component inner : ((JPanel)c).getComponents()) {
                    if (inner instanceof JPanel && "btnPanel".equals(inner.getName())) {
                        btnPanel = inner;
                        break;
                    }
                }
            }
        }
        if (btnPanel != null) {
            btnPanel.setVisible(false);
        }
        
        timerDeposito = new javax.swing.Timer(150, new ActionListener() {
            int progreso = 0;
            int fase = 0; // 0=validando, 1=procesando, 2=completando, 3=exitoso
            
            @Override
            public void actionPerformed(ActionEvent e) {
                progreso += 2;
                progressBar.setValue(progreso);
                progressBar.setString(progreso + "%");
                
                if (fase == 0 && progreso >= 25) {
                    lblEstadoDeposito.setText("Procesando depósito...");
                    fase = 1;
                } else if (fase == 1 && progreso >= 60) {
                    lblEstadoDeposito.setText("Completando transacción...");
                    fase = 2;
                } else if (fase == 2 && progreso >= 85) {
                    lblEstadoDeposito.setText("Realizando depósito final...");
                    fase = 3;
                } else if (progreso >= 100) {
                    timerDeposito.stop();
                    completarDeposito();
                }
            }
        });
        
        timerDeposito.start();
    }
    
    private void completarDeposito() {
        // Realizar el depósito real a la cuenta seleccionada del usuario logueado
        if (cuentaSeleccionada != null) {
            cuentaSeleccionada.saldo += montoDeposito;
        }
        
        // Actualizar UI
        lblEstadoDeposito.setText("¡DEPÓSITO EXITOSO!");
        lblEstadoDeposito.setForeground(new Color(0, 150, 0));
        lblEstadoDeposito.setFont(new Font("SansSerif", Font.BOLD, 20));
        
        progressBar.setValue(100);
        progressBar.setString("Completado");
        progressBar.setForeground(new Color(0, 150, 0));
        
        // Limpiar campos
        tfMontoDepositoEfectivo.setText("");
        
        // Mostrar botones
        Component btnPanel = null;
        for (Component c : ((JPanel)cards.getComponent(2)).getComponents()) {
            if (c instanceof JPanel) {
                for (Component inner : ((JPanel)c).getComponents()) {
                    if (inner instanceof JPanel && "btnPanel".equals(inner.getName())) {
                        btnPanel = inner;
                        break;
                    }
                }
            }
        }
        if (btnPanel != null) {
            btnPanel.setVisible(true);
        }
        
        // Actualizar mensaje en main panel
        lblMensaje.setText("Depósito efectivo de S/ " + df.format(montoDeposito) + 
                         " realizado exitosamente a tu cuenta " + cuentaSeleccionada.numero + ".");
        
        // Actualizar saldo visible
        actualizarSaldoLabel();
        
        // Restablecer colores para próximo uso
        javax.swing.Timer resetTimer = new javax.swing.Timer(100, e -> {
            lblEstadoDeposito.setForeground(Color.BLACK);
            lblEstadoDeposito.setFont(new Font("SansSerif", Font.PLAIN, 18));
            progressBar.setForeground(UIManager.getColor("ProgressBar.foreground"));
        });
        resetTimer.setRepeats(false);
        resetTimer.start();
    }
    
    private void mostrarSaldoActual() {
        if (cuentaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "No hay cuenta seleccionada.", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String mensaje = String.format("Saldo actual de %s (%s):\nS/ %s", 
                                     cuentaSeleccionada.tipo, 
                                     cuentaSeleccionada.numero, 
                                     df.format(cuentaSeleccionada.saldo));
        JOptionPane.showMessageDialog(this, mensaje, "Saldo Actual", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void volverAlMain() {
        cardLayout.show(cards, "MAIN");
    }

    private SubCuenta buscarSubCuentaPorNumero(String numero) {
        for (Persona p : personasPorDni.values()) {
            for (SubCuenta sc : p.cuentas.values()) {
                if (sc.numero.equalsIgnoreCase(numero)) return sc;
            }
        }
        return null;
    }

    private void pedirCodigoYMostrarMisDosSaldos() {
        if (personaActiva == null) return;
        String codigo = JOptionPane.showInputDialog(this, "Ingrese el código para ver sus dos saldos:", "Código requerido", JOptionPane.PLAIN_MESSAGE);
        if (codigo == null) return;
        if (!codigo.equals(CODIGO_SECRET0)) {
            JOptionPane.showMessageDialog(this, "Código incorrecto.", "Código inválido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Saldos de ").append(personaActiva.nombre).append(":\n\n");
        for (SubCuenta sc : personaActiva.cuentas.values()) {
            sb.append(String.format("%s (%s): S/ %s\n", sc.tipo, sc.numero, df.format(sc.saldo)));
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Mis saldos", JOptionPane.INFORMATION_MESSAGE);
    }

    private void logout() {
        // Detener timer si está corriendo
        if (timerDeposito != null && timerDeposito.isRunning()) {
            timerDeposito.stop();
        }
        
        personaActiva = null;
        cuentaSeleccionada = null;
        saldoVisible = false;
        lblBienvenida.setText("Usuario: -");
        lblCuentaNumero.setText("Cuenta: -");
        lblSaldo.setText("Saldo: ****");
        lblMensaje.setText(" ");
        
        // Limpiar campos
        tfMontoInterno.setText("");
        tfMontoToOther.setText("");
        tfDestinoCuenta.setText("");
        tfMontoDepositoEfectivo.setText("");
        
        cardLayout.show(cards, "LOGIN");
    }

    public static void main(String[] args) {
        // Look & feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            CajeroBancoAvanzado app = new CajeroBancoAvanzado();
            app.setVisible(true);
        });
    }
}