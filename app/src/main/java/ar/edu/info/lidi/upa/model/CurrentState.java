package ar.edu.info.lidi.upa.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class CurrentState implements Serializable {

    private Reserva[] reservas;

    public Reserva[] getReservas() {
        return reservas;
    }

    public void setReservas(Reserva[] reservas) {
        this.reservas = reservas;
    }

    public static class Reserva {
        @JsonProperty("docente")
        private Docente docente;
        @JsonProperty("aula")
        private Aula aula;
        @JsonProperty("materia")
        private Materia materia;
        @JsonProperty("horaDesde")
        private Hora horaDesde;
        @JsonProperty("horaHasta")
        private Hora horaHasta;

        public Docente getDocente() {
            return docente;
        }

        public void setDocente(Docente docente) {
            this.docente = docente;
        }

        public Aula getAula() {
            return aula;
        }

        public void setAula(Aula aula) {
            this.aula = aula;
        }

        public Materia getMateria() {
            return materia;
        }

        public void setMateria(Materia materia) {
            this.materia = materia;
        }

        public Hora getHoraDesde() {
            return horaDesde;
        }

        public void setHoraDesde(Hora horaDesde) {
            this.horaDesde = horaDesde;
        }

        public Hora getHoraHasta() {
            return horaHasta;
        }

        public void setHoraHasta(Hora horaHasta) {
            this.horaHasta = horaHasta;
        }
    }


    public static class Docente {
        private String nombre;
        private String apellido;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getApellido() {
            return apellido;
        }

        public void setApellido(String apellido) {
            this.apellido = apellido;
        }
    }

    public static class Aula {
        private String id;
        private String nombre;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
    }

    public static class Materia {
        private String nombre;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
    }

    public static class Hora {
        private String h;
        private String m;

        public String getH() {
            return h;
        }

        public void setH(String h) {
            this.h = h;
        }

        public String getM() {
            return m;
        }

        public void setM(String m) {
            this.m = m;
        }
    }

}
