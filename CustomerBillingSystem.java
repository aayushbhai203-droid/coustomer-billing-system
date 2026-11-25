import java.io.*;
import java.util.Scanner;

/**
 * Customer Billing System (PS No. 49)
 * Concepts: Array, Queue, Searching, Methods, File Handling
 * Bills are stored in bills.txt. First bill ID = BL101. GST = 5%.
 */
public class CustomerBillingSystem {

    static final String FILE_NAME = "bills.txt";
    static final double GST_RATE = 0.05;

    // ---------- Customer record ----------
    static class Customer {
        String id, name, status;   // status: Pending or Paid
        double amount;
        Customer(String id, String name, double amount, String status) {
            this.id = id; this.name = name; this.amount = amount; this.status = status;
        }
        double gst()   { return amount * GST_RATE; }
        double total() { return amount + gst(); }
    }

    // ---------- Array-based circular queue (FIFO) ----------
    static class BillingQueue {
        Customer[] data = new Customer[10];
        int front = 0, size = 0;

        void enqueue(Customer c) {
            if (size == data.length) grow();
            data[(front + size) % data.length] = c;
            size++;
        }
        Customer dequeue() {
            if (size == 0) return null;
            Customer c = data[front];
            data[front] = null;
            front = (front + 1) % data.length;
            size--;
            return c;
        }
        Customer get(int i) { return data[(front + i) % data.length]; }
        boolean isEmpty()   { return size == 0; }
        private void grow() {
            Customer[] bigger = new Customer[data.length * 2];
            for (int i = 0; i < size; i++) bigger[i] = get(i);
            data = bigger; front = 0;
        }
    }

    // ---------- Array of all records (pending + paid) ----------
    static Customer[] records = new Customer[10];
    static int recordCount = 0;
    static BillingQueue queue = new BillingQueue();
    static int nextId = 101;
    static Scanner sc = new Scanner(System.in);

    static void addRecord(Customer c) {
        if (recordCount == records.length) {
            Customer[] bigger = new Customer[records.length * 2];
            for (int i = 0; i < recordCount; i++) bigger[i] = records[i];
            records = bigger;
        }
        records[recordCount++] = c;
    }

    // ---------- Formatting helper (1000.0 -> 1000, 52.5 -> 52.50) ----------
    static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    // ---------- 1. Add customer ----------
    static void addCustomer() {
        System.out.print("Enter customer name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty() || name.contains("|")) {
            System.out.println("Invalid name. Name cannot be blank or contain '|'.");
            return;
        }
        System.out.print("Enter amount: ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount. Amount must be greater than 0.");
            return;
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            System.out.println("Invalid amount. Amount must be greater than 0.");
            return;
        }
        Customer c = new Customer("BL" + nextId++, name, amount, "Pending");
        addRecord(c);
        queue.enqueue(c);
        saveToFile();
        System.out.println("Customer added. ID: " + c.id + " | " + c.name
                + " | Amount: " + fmt(c.amount) + " | Position: " + queue.size);
    }

    // ---------- 2. Process next bill ----------
    static void processNextBill() {
        Customer c = queue.dequeue();
        if (c == null) {
            System.out.println("Queue is empty. No bill to process.");
            return;
        }
        c.status = "Paid";
        saveToFile();
        System.out.println("Bill generated. " + c.id + " | " + c.name + " | Amount: " + fmt(c.amount)
                + " | GST 5%: " + fmt(c.gst()) + " | Total: " + fmt(c.total()));
        if (queue.isEmpty()) System.out.println("Queue is now empty.");
    }

    // ---------- 3. View queue ----------
    static void viewQueue() {
        if (queue.isEmpty()) {
            System.out.println("Queue is empty.");
            return;
        }
        System.out.println("--- WAITING QUEUE ---");
        for (int i = 0; i < queue.size; i++) {
            Customer c = queue.get(i);
            System.out.println((i + 1) + ". " + c.id + " | " + c.name + " | Amount: " + fmt(c.amount));
        }
    }

    // ---------- 4. Search bill by ID (linear search, written manually) ----------
    static int searchById(String id) {
        for (int i = 0; i < recordCount; i++) {
            if (records[i].id.equalsIgnoreCase(id)) return i;
        }
        return -1;
    }

    static void searchBill() {
        System.out.print("Enter bill ID: ");
        String id = sc.nextLine().trim();
        int idx = searchById(id);
        if (idx == -1) {
            System.out.println("Bill " + id + " not found.");
            return;
        }
        Customer c = records[idx];
        System.out.println("Found: " + c.id + " | " + c.name + " | Total: " + fmt(c.total())
                + " | Status: " + c.status);
    }

    // ---------- File handling ----------
    static void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (int i = 0; i < recordCount; i++) {
                Customer c = records[i];
                pw.println(c.id + "|" + c.name + "|" + c.amount + "|" + c.status);
            }
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    static void loadFromFile() {
        File f = new File(FILE_NAME);
        if (!f.exists()) return;
        int maxId = 100;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length != 4) continue;           // skip damaged lines
                try {
                    double amount = Double.parseDouble(p[2]);
                    Customer c = new Customer(p[0], p[1], amount, p[3]);
                    addRecord(c);
                    if (c.status.equals("Pending")) queue.enqueue(c);
                    int num = Integer.parseInt(p[0].substring(2));
                    if (num > maxId) maxId = num;
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    // skip damaged line
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading file: " + e.getMessage());
        }
        nextId = maxId + 1;
    }

    // ---------- Main menu ----------
    public static void main(String[] args) {
        loadFromFile();
        while (true) {
            System.out.println("\n===== Customer Billing System =====");
            System.out.println("1. Add Customer to Queue");
            System.out.println("2. Process Next Bill");
            System.out.println("3. View Queue");
            System.out.println("4. Search Bill by ID");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");
            if (!sc.hasNextLine()) break;
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": addCustomer(); break;
                case "2": processNextBill(); break;
                case "3": viewQueue(); break;
                case "4": searchBill(); break;
                case "5":
                    saveToFile();
                    System.out.println("Data saved to " + FILE_NAME + ". Goodbye!");
                    return;
                default: System.out.println("Invalid choice. Please enter 1-5.");
            }
        }
        saveToFile();
    }
}
