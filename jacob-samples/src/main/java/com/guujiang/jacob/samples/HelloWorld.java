package com.guujiang.jacob.samples;

import com.guujiang.jacob.annotation.Generator;
import com.guujiang.jacob.annotation.GeneratorMethod;

import static com.guujiang.jacob.Stub.yield;

@Generator
public class HelloWorld {

	@GeneratorMethod
	public Iterable<String> sayHello() {
		yield("Hello");
		yield("Welcome to the world of coroutines in Java");
		yield("I am running at the " + Thread.currentThread().getName() + " thread");
		yield("Bye");
		return null;
	}
	
	public static void main(String[] args) {
		HelloWorld hello = new HelloWorld();
		for (String message : hello.sayHello()) {
			System.out.println(message);
		}
	}

}
