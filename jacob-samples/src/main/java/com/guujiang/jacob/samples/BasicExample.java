package com.guujiang.jacob.samples;

import static com.guujiang.jacob.Stub.yield;

import java.util.Iterator;
import java.util.function.Predicate;

import com.guujiang.jacob.annotation.Generator;
import com.guujiang.jacob.annotation.GeneratorMethod;

@Generator
public class BasicExample {

	@GeneratorMethod
	public Iterable<Integer> range(int start, int end) {
		for (int i = start; i < end; ++i) {
			yield(i);
		}
		return null;
	}

	// the generator method can be infinite, just consume as need.
	@GeneratorMethod
	public Iterable<Integer> infiniteFib() {
		int a = 1;
		int b = 1;
		for (;;) {
			yield(a);
			int c = a + b;
			a = b;
			b = c;
		}
	}
	
	// with the coroutine syntax, many functional method can by implemented in a straightforward way
	
	@GeneratorMethod
	public <T> Iterable<T> filter(Iterable<T> source, Predicate<T> predicate) {
		// A issue occurs with the for-loop syntax that will be fixed in next version
		// for now just use the old school iterator syntax
		Iterator<T> iter = source.iterator();
		while (iter.hasNext()) {
			T val = iter.next();
			if (val != null && predicate.test(val)) {
				yield(val);
			}
		}
		return null;
	}

	@GeneratorMethod
	public <T> Iterable<T> take(Iterable<T> source, int n) {
		Iterator<T> iter = source.iterator();
		int i = 0;
		while (iter.hasNext() && (i < n)) {
			yield(iter.next());
			++i;
		}
		return null;
	}

	public static void main(String[] args) {
		BasicExample ex = new BasicExample();
		
		// the generator method can accept arguments.
		ex.range(5, 10).forEach(System.out::println);
		
		System.out.println("the first 10 even numbers in the fibonacci series: ");
		ex.take(ex.filter(ex.infiniteFib(), x -> x % 2 == 0), 10).forEach(System.out::println);
	}
}
