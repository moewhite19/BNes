package cn.whiteg.bnes.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;

import java.lang.reflect.Field;
import java.util.Arrays;

public class NMSUtils {
    //根据类型获取Field
    public static Field getFieldFormType(Class<?> clazz,Class<?> type) throws NoSuchFieldException {
        for (Field declaredField : clazz.getDeclaredFields()) {
            if (declaredField.getType().equals(type)) {
                declaredField.setAccessible(true);
                return declaredField;
            }
        }

        //如果有父类 检查父类
        var superClass = clazz.getSuperclass();
        if (superClass != null) return getFieldFormType(superClass,type);
        throw new NoSuchFieldException(type.getName());
    }

    //根据类型获取Field(针对泛型)
    public static Field getFieldFormType(Class<?> clazz,String type) throws NoSuchFieldException {
        for (Field declaredField : clazz.getDeclaredFields()) {
            if (declaredField.getAnnotatedType().getType().getTypeName().equals(type)) return declaredField;
        }
        //如果有父类 检查父类
        var superClass = clazz.getSuperclass();
        if (superClass != null) return getFieldFormType(clazz,type);
        throw new NoSuchFieldException(type);
    }

    //从数组结构中查找Field
    public static Field[] getFieldFormStructure(Class<?> clazz,Class<?>... types) throws NoSuchFieldException {
        var fields = clazz.getDeclaredFields();
        Field[] result = new Field[types.length];
        int index = 0;
        for (Field f : fields) {
            if (f.getType() == types[index]){
                result[index] = f;
                index++;
                if (index >= types.length){
                    return result;
                }
            } else {
                index = 0;
            }
        }
        throw new NoSuchFieldException(Arrays.toString(types));
    }

    //根据实体Class获取实体Types
    public static <T extends Entity> EntityTypes<T> getEntityType(Class<? extends Entity> clazz) {
        String name = EntityTypes.class.getName().concat("<").concat(clazz.getName()).concat(">");
        for (Field field : EntityTypes.class.getFields()) {
            try{
                if (field.getAnnotatedType().getType().getTypeName().equals(name))
                    //noinspection unchecked
                    return (EntityTypes<T>) field.get(null);
            }catch (IllegalAccessException e){
                e.printStackTrace();
            }
        }
        return null;
    }

}
