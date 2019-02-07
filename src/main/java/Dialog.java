import javax.swing.*;
import java.awt.*;

public class Dialog extends JDialog {

    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel mesLabel;

    public Dialog(String mes) {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width/2-getSize().width, dim.height/2-getSize().height);
        setSize(500, 200);
        setContentPane(contentPane);
        mesLabel.setText(mes);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> dispose());
        setVisible(true);
    }

}
