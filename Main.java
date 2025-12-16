import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    static class Department {
        private final int id;
        private final String name;

        Department(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public String getName() { return name; }

        @Override
        public String toString() {
            return "Department{id=" + id + ", name='" + name + "'}";
        }
    }

    enum Gender {
        MALE, FEMALE;

        static Gender parse(String s) {
            if (s == null) return null;
            String v = s.trim().toLowerCase(Locale.ROOT);
            return switch (v) {
                case "m", "male", "man", "м", "муж", "мужчина" -> MALE;
                case "f", "female", "woman", "ж", "жен", "женщина" -> FEMALE;
                default -> throw new IllegalArgumentException("Unknown gender: " + s);
            };
        }
    }

    static class Person {
        private final int id;
        private final String name;
        private final Gender gender;
        private final Department department;
        private final double salary;
        private final LocalDate birthDate;

        Person(int id, String name, Gender gender, Department department, double salary, LocalDate birthDate) {
            this.id = id;
            this.name = name;
            this.gender = gender;
            this.department = department;
            this.salary = salary;
            this.birthDate = birthDate;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public Gender getGender() { return gender; }
        public Department getDepartment() { return department; }
        public double getSalary() { return salary; }
        public LocalDate getBirthDate() { return birthDate; }

        @Override
        public String toString() {
            return "Person{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", gender=" + gender +
                    ", department=" + department +
                    ", salary=" + salary +
                    ", birthDate=" + birthDate +
                    '}';
        }
    }

    static final char SEP = ';';
    static final DateTimeFormatter BIRTH_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static List<Person> loadPeopleFromCsv(File file) throws IOException {
        Map<String, Department> departmentsByKey = new LinkedHashMap<>();
        int nextDepartmentId = 1;

        List<Person> people = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;

            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty()) continue;

                if (lineNo == 1 && looksLikeHeader(line)) continue;

                List<String> cols = splitCsvLine(line, SEP);
                if (cols.size() < 6) {
                    throw new IllegalArgumentException("Line " + lineNo + ": expected 6 columns, got " + cols.size()
                            + ". Line: " + line);
                }
                if (lineNo == 1 && !isInt(cols.get(0))) {
                    continue;
                }

                int personId = parseInt(cols.get(0), "id", lineNo);
                String name = cols.get(1).trim();
                Gender gender = Gender.parse(cols.get(2));

                LocalDate birthDate = parseBirthDate(cols.get(3), lineNo); // BirtDate
                String depNameRaw = cols.get(4).trim();                    // Division
                double salary = parseDouble(cols.get(5), "salary", lineNo);// Salary

                String depKey = normalizeDepartmentName(depNameRaw);
                Department dep = departmentsByKey.get(depKey);
                if (dep == null) {
                    dep = new Department(nextDepartmentId++, depNameRaw);
                    departmentsByKey.put(depKey, dep);
                }

                people.add(new Person(personId, name, gender, dep, salary, birthDate));
            }
        }

        return people;
    }

    static boolean isInt(String s) {
        if (s == null) return false;
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean looksLikeHeader(String line) {
        String l = line.toLowerCase(Locale.ROOT);
        return (l.contains("id") || l.contains("ид")) &&
                (l.contains("name") || l.contains("имя")) &&
                (l.contains("department") || l.contains("подраздел"));
    }

    static String normalizeDepartmentName(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    static int parseInt(String s, String field, int lineNo) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Line " + lineNo + ": bad " + field + " = " + s);
        }
    }

    static double parseDouble(String s, String field, int lineNo) {
        try {
            String v = s.trim().replace(',', '.');
            return Double.parseDouble(v);
        } catch (Exception e) {
            throw new IllegalArgumentException("Line " + lineNo + ": bad " + field + " = " + s);
        }
    }

    static LocalDate parseBirthDate(String s, int lineNo) {
        try {
            return LocalDate.parse(s.trim(), BIRTH_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Line " + lineNo + ": bad birthDate (expected dd.MM.yyyy) = " + s);
        }
    }

    static List<String> splitCsvLine(String line, char sep) {
        List<String> out = new ArrayList<>(8);
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); // "" -> "
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == sep && !inQuotes) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().trim());
        return out;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Working dir: " + new java.io.File(".").getAbsolutePath());
        System.out.println("CSV exists: " + new java.io.File("foreign_names.csv").exists());
        File csv = (args.length >= 1) ? new File(args[0]) : new File("foreign_names.csv");

        List<Person> people = loadPeopleFromCsv(csv);

        for (Person person: people) {
            System.out.println(person + "\n");
        }
        
        System.out.println("Loaded people: " + people.size());

        Set<Integer> depIds = new HashSet<>();
        for (Person p : people) depIds.add(p.getDepartment().getId());
        System.out.println("Unique departments: " + depIds.size());
    }
}