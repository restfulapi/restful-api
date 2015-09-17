package net.hedtech.restfulapp

class Parent {
    String name

    static hasMany = [children: Child]


    static constraints = {
    }
}
