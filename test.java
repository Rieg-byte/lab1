import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;


//Реализовали возможность рисования фрактала с несколькими фоновыми потоками.
//Благодоря чему интерфейс не зависает во время рисования фрактала, а сам процесс рисования
//происходит намного быстрее за счет многоядерного процессора
/**
 * Класс исследует различные области фрактала, путем его создания,
 * отображения через графический интерфейс Swing и обработки событий,
 * вызванных взаимодействием приложения с пользователем
 */
public class FractalExplorer {
    private int size; //Размер экрана
    private JImageDisplay jDisplay; //Ссылка для обновления отображения в разных методах в процессе вычисления фрактала
    private FractalGenerator fractal; //Будет использоваться ссылка на базовый класс для отображения других видов фракталов в будущем
    private Rectangle2D.Double range; //Объект, указывающий диапазон комплексной плоскости, которая выводится на экран

    JComboBox comboBox;
    JButton resetButton;
    JButton saveButton;

    private int rowsRemaining;
    /**
     * конструктор, который принимает значение размера отображения в качестве аргумента,
     * затем сохраняет это значение в соответствующем поле, а также инициализует объекты
     * диапазона и фрактального генератора
     * @param display_size
     */
    public FractalExplorer (int display_size) {
        size = display_size;
        range = new Rectangle2D.Double();
        fractal = new Mandelbrot();
        fractal.getInitialRange(range);
        jDisplay = new JImageDisplay(display_size, display_size);

    }

    /**
     * Метод инициализирует графический интерфейс Swing: JFrame,
     * содержащий объект JimageDisplay, и кнопку для сброса отображения.
     */
    public void createAndShowGUI () {
        jDisplay.setLayout(new BorderLayout());
        JFrame frame = new JFrame();

        frame.add(jDisplay, BorderLayout.CENTER);

        resetButton = new JButton("Reset");
        ResetButtonHandler clearAction = new ResetButtonHandler();
        resetButton.addActionListener(clearAction);

        MyMouseListener mouse = new MyMouseListener();
        jDisplay.addMouseListener(mouse);

        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);

        String[] items = {"Mandelbrot", "Tricorn", "BurningShip"};
        comboBox = new JComboBox(items);

        JLabel label = new JLabel("Fractal:");
        JPanel panelBox = new JPanel();
        panelBox.add(label);
        panelBox.add(comboBox);
        frame.add(panelBox, BorderLayout.NORTH);

        ChooseButtonHandler chooseAction = new ChooseButtonHandler();
        comboBox.addActionListener(chooseAction);

        saveButton = new JButton("Save Image");
        SaveImageButton saveAction = new SaveImageButton();
        saveButton.addActionListener(saveAction);

        JPanel panelButtons = new JPanel();
        panelButtons.add(resetButton);
        panelButtons.add(saveButton);
        frame.add(panelButtons, BorderLayout.SOUTH);


        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);
    }

    /**
     * Метод с типом доступа private для вывода на экран фрактала
     */
    private void drawFractal () {
        //отключает все элементы UI во время рисования
        enableIO(false);
        //равна количеству строк, которые нужно нарисовать
        rowsRemaining = size;
        //для каждой строки в отображении создается
        //отдельный рабочий объект, а затем вызывается метод execute
        //Это действие запускает фоновый поток и задачу в фоновом режиме.
        for (int y = 0; y < size; y++){
            FractalWorker frac = new FractalWorker(y);
            frac.execute();
        }
    }
    //включает и выключает кнопки с выпадающим списком. Для вкл и откл используется функция setEnabled
    private void enableIO(boolean val){
        comboBox.setEnabled(val);
        resetButton.setEnabled(val);
        saveButton.setEnabled(val);
    }
    /**
     * Класс для обработки работы мыши дисплея
     */
    private class MyMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            //возвращает предыдущее состояние
            if (rowsRemaining!=0) return;
            int x = e.getX();
            double xCoord = fractal.getCoord(range.x, range.x+range.width, size,x);
            int y = e.getY();
            double yCoord = fractal.getCoord(range.y, range.y+range.height, size,y);
            fractal.recenterAndZoomRange(range, xCoord, yCoord, 0.5);
            drawFractal();
        }
    }

    /**
     * Класс для обработки кнопки сброса
     */
    public class ResetButtonHandler implements ActionListener {
        public void actionPerformed (ActionEvent e) {
            fractal.getInitialRange(range);
            drawFractal();
        }
    }

    /**
     * Класс для обработки кнопки выбора с выпадающим списком
     */
    public class ChooseButtonHandler implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            JComboBox combo = (JComboBox)e.getSource();
            String name = (String) combo.getSelectedItem();
            if (name == "Mandelbrot"){
                fractal = new Mandelbrot();
            }
            if (name == "Tricorn") {
                fractal = new Tricorn();
            }
            if (name == "BurningShip") {
                fractal = new BurningShip();
            }
            fractal.getInitialRange(range);
            drawFractal();
        }
    }

    /**
     * Класс для обработки кнопки сохранения изображения
     */
    public class SaveImageButton implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            FileFilter filter = new FileNameExtensionFilter("PNG Images", "png");
            chooser.setFileFilter(filter);
            chooser.setAcceptAllFileFilterUsed(false);

            int result = chooser.showSaveDialog(jDisplay);
            if (result == JFileChooser.APPROVE_OPTION) {
                File dir = chooser.getSelectedFile();
                String dir_string = dir.toString();
                try{
                    BufferedImage image = jDisplay.getImage();
                    ImageIO.write(image, "png", dir);
                }
                catch(Exception exception){
                    JOptionPane.showMessageDialog(chooser, exception.getMessage(),"Can not save image", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    //SwingWorker облегчает процесс фонового потока.
    //FractalWorker Отвечает за генерацию
    //данных строки и за рисование данной строки
    private class FractalWorker extends SwingWorker<Object,Object>{
        private int y;//координата вычисляемой строки
        private int[] valuesRGB; //нужен для хранения значений RGB для каждого пикселя в этой строке
        //конструктор получает y координату в качестве параментра и сохраняет его
        public FractalWorker (int y){
            this.y = y;
        }

        //Переопределяем метод
        //Данный метод вызывается в фоновом потоке и отвечает за выполнение
        //длительной задачи.
        protected Object doInBackground() throws Exception {
            valuesRGB = new int[size];//масив целых чисел который сохраняет цвет каждого пикселя
            //цикл сохраняет каждое значение RGB в соответствующем элементе целочисленного массива
            for (int x = 0; x < valuesRGB.length; x ++) {
                //вычисление фрактала для указанной строки без обновления отображения
                double xCoord = fractal.getCoord(range.x,range.x + range.width, size, x);
                double yCoord = fractal.getCoord(range.y, range.y + range.height, size, y);
                int iterations = fractal.numIterations(xCoord,yCoord);
                int rgbColor;
                if (iterations == -1)
                    rgbColor = 0;
                else {
                    float hue = 0.7f + (float) iterations / 200f;
                    rgbColor = Color.HSBtoRGB(hue, 1f, 1f);
                }
                valuesRGB[x] = rgbColor;
            }
            return null;
        }

        //Метод вызывает, когда фоновая задача завершена, и этот метод вызывается из потока
        //обработки событий Swing
        protected void done() {
            //цикл перебирает массив строк данных, рисуя пиксели, которые были вычислены
            //в прерыдущем методе
            for (int i = 0; i < valuesRGB.length; i ++) {
                jDisplay.drawPixel(i,y,valuesRGB[i]);
            }
            //перерисововывает изображение
            jDisplay.repaint(0,0,y,size,1);
            rowsRemaining--;//уменьшает
            if (rowsRemaining == 0) //включает элементы пользовательского интерфейса// если кол.строк равно 0
                enableIO(true);
        }
    }



    public static void main (String[] args) {
        FractalExplorer display = new FractalExplorer(300);
        display.createAndShowGUI();
        display.drawFractal();
    }
}
