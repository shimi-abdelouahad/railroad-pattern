package com.as.lexer;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@FunctionalInterface
public interface Lexer<T> {




    Optional<T> tryParse(String text);

    static <T> Lexer<T> create() {
        return text -> {
            requireNonNull(text);
            return Optional.empty();
        };
    }

    static Lexer<String> from(String regex) {
        var pattern = Pattern.compile(regex);
        return from(pattern);
    }

    static Lexer<String> from(Pattern pattern) {
        requireNonNull(pattern);
        requireOneGroup(pattern);
        return text -> {
            requireNonNull(text);
            return Optional.of(pattern.matcher(text)).filter(Matcher::matches).map(matcher -> matcher.group(1));
        };
    }

    private static void requireOneGroup(Pattern pattern) {
        if(pattern.matcher("").groupCount()!=1){
            throw new IllegalArgumentException();
        }
    }

    default <R> Lexer<R> map(Function<? super T, ? extends R> mapper){
        requireNonNull(mapper);
        return text -> tryParse(text).map(mapper);
    }

    default  Lexer<T> or(Lexer<? extends T> lexer){
        requireNonNull(lexer);
        return text -> tryParse(text).or(()->lexer.tryParse(text));
    }

    default Lexer<T> with(String regx, Function<? super String, ? extends T> mapper){
        return or(from(regx).map(mapper));
    }


    static <T> Lexer<T> form(List<String> regexes, List< Function<? super String, ? extends T>> mappers){
        requireNonNull(regexes);
        requireNonNull(mappers);
        if(regexes.size()!=mappers.size()){
            throw new IllegalArgumentException();
        }
        return new Lexer<T>() {
            @Override
            public Optional<T> tryParse(String text) {
                requireNonNull(text);
                if( regexes.isEmpty() ){
                    return Optional.empty();
                }
                var matcher = Pattern.compile(regexes.stream().collect(Collectors.joining("|"))).matcher(text);
                if(!matcher.matches()){
                    return Optional.empty();
                }
                for(var i =0; i< matcher.groupCount(); i++){
                    var group = matcher.group(i+1);
                    if(group!=null){
                        return Optional.of(group).map(mappers.get(i));
                    }
                }
                return Optional.empty();
            }
        };
    }

}
