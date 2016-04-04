package org.unicen.dmetrics.firebase.core;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.unicen.dmetrics.firebase.json.DateDeserializer;
import org.unicen.dmetrics.firebase.json.SetAdapterFactory;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component
public class FirebaseTemplate {
	
	@Autowired
	private FirebaseAnnotationProcessor annotationProcessor;

	@Autowired
	private ReflectionHelper reflectionHelper;

	@Autowired
	private FirebasePlaceholdersProcessor placeholdersProcessor;

	private final FirebaseConfiguration config;
	private final RestTemplate restTemplate;
	private final Gson gson;
		
	@Autowired
	public FirebaseTemplate(FirebaseConfiguration config) {
		this.config = config;
		this.restTemplate = new RestTemplate();
	
		GsonBuilder builder = new GsonBuilder()
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
				.setDateFormat(DateFormat.LONG)
				.registerTypeAdapter(Date.class, new DateDeserializer())
				.registerTypeAdapterFactory(new SetAdapterFactory());
		
	    this.gson = builder.create();
	}

	public <T> Optional<T> findByKey(Class<T> domainClass, String key){

		String entityPath = annotationProcessor.getPathUrl(domainClass);
		
		List<String> urlPlaceholders = placeholdersProcessor.getUrlPlaceholders(entityPath);
		if(!urlPlaceholders.isEmpty()){
			throw new IllegalStateException(String.format("Find by Key in class %s contains Url placeholders", domainClass));
		}
		
		Field keyField = annotationProcessor.getKeyField(domainClass);

		String url = config.getHost() + entityPath + "/{key}.json";
		String json = restTemplate.getForObject(url, String.class, key);

		Optional<T> result = Optional.ofNullable(json)
				.map(jsonString -> gson.fromJson(jsonString, domainClass));
		result.ifPresent(instance -> reflectionHelper.setPrivateField(instance, keyField, key));

		return result;
	}

	public <T> Iterable<T> findAll(Class<T> domainClass){

		List<T> list = new ArrayList<>();

		String entityPath = annotationProcessor.getPathUrl(domainClass);
		List<String> urlPlaceholders = placeholdersProcessor.getUrlPlaceholders(entityPath);
		if(!urlPlaceholders.isEmpty()){
			throw new IllegalStateException(String.format("Find by Key in class %s contains Url placeholders", domainClass));
		}
		
		String url = config.getHost() + entityPath + ".json";
		
//		Map<String, T> entity = restTemplate.getForObject(url, domainClass);
		
		return list;
	}
}