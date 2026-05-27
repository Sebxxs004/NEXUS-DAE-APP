package com.prisma.models;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Caso {
    private String nombre;
    private String descripcion;
    private String lugar;
    private LocalDate fechaHechos;
    private List<String> victimas;
    private List<String> victimarios;
    private List<String> delitos;
    private List<String> actoresInvolucrados;
    private String imagenPath;

    /** Constructor completo para casos con todos los metadatos. */
    public Caso(String nombre, String descripcion, String lugar, LocalDate fechaHechos,
                List<String> victimas, List<String> victimarios, List<String> delitos,
                List<String> actoresInvolucrados) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.fechaHechos = fechaHechos;
        this.victimas = new ArrayList<>(victimas);
        this.victimarios = new ArrayList<>(victimarios);
        this.delitos = new ArrayList<>(delitos);
        this.actoresInvolucrados = new ArrayList<>(actoresInvolucrados);
        this.imagenPath = null;
    }

    /** Constructor simplificado para casos generados desde la carpeta casos/. */
    public Caso(String nombre, String imagenPath) {
        this.nombre = nombre;
        this.imagenPath = imagenPath;
        this.descripcion = "Caso cargado desde imagen.";
        this.lugar = "Sin especificar";
        this.fechaHechos = LocalDate.now();
        this.victimas = new ArrayList<>();
        this.victimarios = new ArrayList<>();
        this.delitos = new ArrayList<>();
        this.actoresInvolucrados = new ArrayList<>();
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }
    public LocalDate getFechaHechos() { return fechaHechos; }
    public void setFechaHechos(LocalDate fechaHechos) { this.fechaHechos = fechaHechos; }
    public List<String> getVictimas() { return victimas; }
    public void setVictimas(List<String> victimas) { this.victimas = new ArrayList<>(victimas); }
    public List<String> getVictimarios() { return victimarios; }
    public void setVictimarios(List<String> victimarios) { this.victimarios = new ArrayList<>(victimarios); }
    public List<String> getDelitos() { return delitos; }
    public void setDelitos(List<String> delitos) { this.delitos = new ArrayList<>(delitos); }
    public List<String> getActoresInvolucrados() { return actoresInvolucrados; }
    public void setActoresInvolucrados(List<String> actoresInvolucrados) { this.actoresInvolucrados = new ArrayList<>(actoresInvolucrados); }
    public String getImagenPath() { return imagenPath; }
    public void setImagenPath(String imagenPath) { this.imagenPath = imagenPath; }
    public boolean tieneImagen() { return imagenPath != null && !imagenPath.isBlank(); }

    public String getFechaHechosFormateada() {
        return fechaHechos != null ? fechaHechos.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Sin fecha";
    }

    public String getResumenCorto() {
        return nombre + " · " + lugar + " · " + getFechaHechosFormateada();
    }

    public String getTituloNodo() {
        return nombre + "\n" + lugar;
    }

    public String getDetallado() {
        return "Descripción: " + descripcion + "\n"
                + "Lugar: " + lugar + "\n"
                + "Fecha: " + getFechaHechosFormateada() + "\n"
                + "Víctimas: " + String.join(", ", victimas) + "\n"
                + "Victimarios: " + String.join(", ", victimarios) + "\n"
                + "Delitos: " + String.join(", ", delitos) + "\n"
                + "Actores: " + String.join(", ", actoresInvolucrados);
    }
}
