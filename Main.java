import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

enum Estado { SUSCEPTIBLE, EXPUESTO, INFECTADO, RECUPERADO }

class Persona {
    int x, y;
    int velocidadX, velocidadY;
    Estado estado;
    int diasExposicion, diasInfectado;
    int duracionExposicion, duracionInfeccion;
    Model model;

    public Persona(int xInit, int yInit, Estado estado, int duracionExposicion, int duracionInfeccion, Model model) {
        this.x = xInit;
        this.y = yInit;
        this.estado = estado;
        this.diasExposicion = 0;
        this.diasInfectado = 0;
        this.duracionExposicion = duracionExposicion;
        this.duracionInfeccion = duracionInfeccion;
        this.model = model;

        Random rand = new Random();
        this.velocidadX = rand.nextInt(3) - 1; // -1, 0, 1
        this.velocidadY = rand.nextInt(3) - 1; // -1, 0, 1
    }

    public void mover() {
        // Actualizar posiciones según velocidades
        x += velocidadX;
        y += velocidadY;

        // Revertir velocidad si alcanzan los límites
        if (x < 0 || x > 200) velocidadX = -velocidadX;
        if (y < 0 || y > 200) velocidadY = -velocidadY;
    }

    public boolean estaCerca(Persona otra) {
        // Calcular la distancia entre dos personas usando ChocoSolver
        int distanciaX = this.x - otra.x;
        int distanciaY = this.y - otra.y;
        int distanciaCuadrada = distanciaX * distanciaX + distanciaY * distanciaY;

        // Verificar si están dentro del rango de proximidad
        return distanciaCuadrada <= 100;
    }

    public void actualizarEstado() {
        if (estado == Estado.EXPUESTO) {
            diasExposicion++;
            if (diasExposicion >= duracionExposicion) {
                estado = Estado.INFECTADO;
                diasExposicion = 0;
            }
        } else if (estado == Estado.INFECTADO) {
            diasInfectado++;
            if (diasInfectado >= duracionInfeccion) {
                estado = Estado.RECUPERADO;
                diasInfectado = 0;
            }
        }
    }
}

class SimulacionPanel extends JPanel {
    private ArrayList<Persona> personas;
    private XYSeries serieSusceptibles, serieExpuestos, serieInfectados, serieRecuperados;
    private int tasaContagio, dia;
    private Model model;

    public SimulacionPanel(int poblacion, int infectadosIniciales, int tasaContagio, int duracionExposicion, int duracionInfeccion) {
        this.tasaContagio = tasaContagio;
        this.model = new Model("Simulación");
        personas = new ArrayList<>();
        serieSusceptibles = new XYSeries("Susceptibles");
        serieExpuestos = new XYSeries("Expuestos");
        serieInfectados = new XYSeries("Infectados");
        serieRecuperados = new XYSeries("Recuperados");
        dia = 0;

        // Inicializar personas
        Random rand = new Random();
        for (int i = 0; i < poblacion; i++) {
            int x = rand.nextInt(200);
            int y = rand.nextInt(200);
            Estado estado = (i < infectadosIniciales) ? Estado.INFECTADO : Estado.SUSCEPTIBLE;
            personas.add(new Persona(x, y, estado, duracionExposicion, duracionInfeccion, model));
        }

        // Timer para actualizar la simulación
        Timer timer = new Timer(100, e -> {
            moverPersonas();
            contagiarYRecuperar();
            actualizarGrafica();
            repaint();
        });
        timer.start();
    }

    private void moverPersonas() {
        for (Persona persona : personas) {
            persona.mover();
        }
    }

    private void contagiarYRecuperar() {
        Random rand = new Random();
        for (Persona p1 : personas) {
            if (p1.estado == Estado.INFECTADO) {
                for (Persona p2 : personas) {
                    if (p2.estado == Estado.SUSCEPTIBLE && p1.estaCerca(p2) && rand.nextInt(100) < tasaContagio) {
                        p2.estado = Estado.EXPUESTO;
                    }
                }
            }
            p1.actualizarEstado();
        }
    }

    private void actualizarGrafica() {
        int susceptibles = 0, expuestos = 0, infectados = 0, recuperados = 0;
        for (Persona persona : personas) {
            switch (persona.estado) {
                case SUSCEPTIBLE -> susceptibles++;
                case EXPUESTO -> expuestos++;
                case INFECTADO -> infectados++;
                case RECUPERADO -> recuperados++;
            }
        }
        serieSusceptibles.add(dia, (susceptibles * 100) / personas.size());
        serieExpuestos.add(dia, (expuestos * 100) / personas.size());
        serieInfectados.add(dia, (infectados * 100) / personas.size());
        serieRecuperados.add(dia, (recuperados * 100) / personas.size());
        dia++;
    }

    public XYSeriesCollection getDataset() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(serieSusceptibles);
        dataset.addSeries(serieExpuestos);
        dataset.addSeries(serieInfectados);
        dataset.addSeries(serieRecuperados);
        return dataset;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.BLACK);

        for (Persona persona : personas) {
            switch (persona.estado) {
                case SUSCEPTIBLE -> g.setColor(Color.CYAN);
                case EXPUESTO -> g.setColor(Color.ORANGE);
                case INFECTADO -> g.setColor(Color.RED);
                case RECUPERADO -> g.setColor(Color.GRAY);
            }
            g.fillOval(persona.x, persona.y, 10, 10);
        }
    }
}

public class Main extends JFrame {
    private int numPaises;
    private JTextField[] populationFields, initialInfectedFields;
    private JSlider contagionRateSlider;
    private JTextField diseaseDurationField, exposureDurationField;
    private JPanel simulacionContainer;

    public Main(int numPaises) {
        this.numPaises = numPaises;

        setTitle("Simulador de Propagación Multiagente SEIR");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        contagionRateSlider = new JSlider(0, 100, 10);
        JLabel contagionRateLabel = new JLabel("Tasa de Contagio: 10%");
        contagionRateSlider.addChangeListener(e -> contagionRateLabel.setText("Tasa de Contagio: " + contagionRateSlider.getValue() + "%"));
        controlPanel.add(contagionRateLabel);
        controlPanel.add(contagionRateSlider);

        JLabel exposureDurationLabel = new JLabel("Duración de Exposición (Días):");
        exposureDurationField = new JTextField("5", 5);
        controlPanel.add(exposureDurationLabel);
        controlPanel.add(exposureDurationField);

        JLabel diseaseDurationLabel = new JLabel("Duración de la Enfermedad (Días):");
        diseaseDurationField = new JTextField("10", 5);
        controlPanel.add(diseaseDurationLabel);
        controlPanel.add(diseaseDurationField);

        populationFields = new JTextField[numPaises];
        initialInfectedFields = new JTextField[numPaises];
        for (int i = 0; i < numPaises; i++) {
            JPanel countryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel countryLabel = new JLabel("País " + (i + 1) + ": ");
            JLabel populationLabel = new JLabel("Población");
            JLabel infectedLabel = new JLabel("Infectados Iniciales");

            populationFields[i] = new JTextField("100", 5);
            initialInfectedFields[i] = new JTextField("10", 5);

            countryPanel.add(countryLabel);
            countryPanel.add(populationLabel);
            countryPanel.add(populationFields[i]);
            countryPanel.add(infectedLabel);
            countryPanel.add(initialInfectedFields[i]);

            controlPanel.add(countryPanel);
        }

        JButton initializeButton = new JButton("Iniciar Simulación");
        initializeButton.addActionListener(e -> startSimulacion());
        controlPanel.add(initializeButton);

        add(controlPanel, BorderLayout.NORTH);

        simulacionContainer = new JPanel();
        simulacionContainer.setLayout(new GridLayout(numPaises, 2));
        add(simulacionContainer, BorderLayout.CENTER);
    }

    private void startSimulacion() {
        simulacionContainer.removeAll();
        int tasaContagio = contagionRateSlider.getValue();
        int duracionExposicion = Integer.parseInt(exposureDurationField.getText());
        int duracionInfeccion = Integer.parseInt(diseaseDurationField.getText());

        for (int i = 0; i < numPaises; i++) {
            int poblacion = Integer.parseInt(populationFields[i].getText());
            int infectadosIniciales = Integer.parseInt(initialInfectedFields[i].getText());

            SimulacionPanel simulacionPanel = new SimulacionPanel(poblacion, infectadosIniciales, tasaContagio, duracionExposicion, duracionInfeccion);
            simulacionContainer.add(simulacionPanel);

            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Progreso de Infección - País " + (i + 1),
                    "Días",
                    "% de Población",
                    simulacionPanel.getDataset(),
                    PlotOrientation.VERTICAL,
                    true, true, false);
            ChartPanel chartPanel = new ChartPanel(chart);
            simulacionContainer.add(chartPanel);
        }

        simulacionContainer.revalidate();
        simulacionContainer.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main(3).setVisible(true));
    }
}