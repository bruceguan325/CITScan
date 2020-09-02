package com.intumit.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class PoiUtil {

    public static void setHeader(Workbook workBook, String[] header) {
        Sheet sheet = workBook.createSheet();
        Row sheetHeader = sheet.getRow(0);
        if (sheetHeader == null) {
            sheetHeader = sheet.createRow(0);
        }
        for (int x = 0; x < header.length; x++) {
            Cell headerCell = sheetHeader.getCell(x);
            if (headerCell == null) {
                headerCell = sheetHeader.createCell(x);
            }
            headerCell.setCellType(CellType.STRING);
            headerCell.setCellValue(header[x]);
        }
    }

    public static void setRows(Workbook workBook, List<?> list) throws NullPointerException {
        Sheet sheet = workBook.getSheetAt(0);
        if (sheet == null) {
            throw new NullPointerException("Sheet is null");
        }
        Row sheetHeader = sheet.getRow(sheet.getFirstRowNum());
        int headerLength = sheetHeader.getPhysicalNumberOfCells();
        int lastRow = sheet.getPhysicalNumberOfRows();
        for (int i = 0; i < list.size(); i++) {
            Row row = sheet.createRow(i + lastRow);
            Object bean = list.get(i);
            if (bean == null)
            	continue;
            Class<?> clazz = bean.getClass();
            for (int j = 0; j < headerLength; j++) {
                String headerName = sheetHeader.getCell(j).getStringCellValue();
                Method method;
                try {
                    method = PoiUtil.checkMethod(headerName, clazz);
                }
                catch (Exception e) {
                    continue;
                }
                Object result;
                try {
                    result = method.invoke(bean);
                    Cell cell = row.createCell(j);
                    PoiUtil.setCell(cell, result);
                }
                catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    System.out.println("Invoke method fail,Methhod:" + method.getName());
                    return;
                }
            }
        }
    }

    public static void createSheet(Workbook workBook, String[] header, List<?> list)
            throws NullPointerException {
        setHeader(workBook, header);
        setRows(workBook, list);
    }

    private static Boolean isLegalGetMethod(final Method method) {
        if (method == null) {
            return false;
        }
        final int modifiers = method.getModifiers();
        if (method.getParameterTypes().length == 0 && Modifier.isPublic(modifiers)
                && !Modifier.isStatic(modifiers) && !method.isBridge()) {
            return true;
        }
        else {
            return false;
        }

    }

    private static void setCell(Cell cell, Object result) {
        if (result == null) {
            cell.setCellType(CellType.BLANK);
        }
        else {
            if (result instanceof Integer) {
                Integer iResult = (Integer)result;
                cell.setCellType(CellType.NUMERIC);
                cell.setCellValue(iResult);
            }
            else if (result instanceof Long) {
                Long lResult = (Long)result;
                cell.setCellType(CellType.NUMERIC);
                cell.setCellValue(lResult);
            }
            else if (result instanceof Double) {
                Double dResult = (Double)result;
                cell.setCellType(CellType.NUMERIC);
                cell.setCellValue(dResult);
            }
            else if (result instanceof String) {
                String sResult = StringUtils.substring((String)result, 0, 32767);
                cell.setCellType(CellType.STRING);
                cell.setCellValue(sResult);
            }
            else if (result instanceof Date) {
                Date dResult = null;
                if (result.getClass() == Timestamp.class) {
                    Timestamp ts = (Timestamp)result;
                    dResult = new Date(ts.getTime());
                }
                else {
                    dResult = (Date)result;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                cell.setCellType(CellType.STRING);
                cell.setCellValue(sdf.format(dResult));
            }
            else if (result instanceof Boolean) {
                Boolean bResult = (Boolean)result;
                cell.setCellType(CellType.BOOLEAN);
                cell.setCellValue(bResult);
            }
            else if (result instanceof RichTextString) {
                RichTextString RTSResult = (RichTextString)result;
                cell.setCellType(CellType.STRING);
                cell.setCellValue(RTSResult);
            }
        }

    }

    private static Method checkMethod(String methodName, Class<?> clazz)
            throws Exception {
        methodName = methodName.toUpperCase().charAt(0) + methodName.substring(1);
        Method method = null;
        try {
            method = clazz.getMethod("get" + methodName);
        }
        catch (NoSuchMethodException | SecurityException e) {
            try {
                method = clazz.getMethod("is" + methodName);
            }
            catch (NoSuchMethodException | SecurityException exception) {
                throw new Exception("IllegalGetMethod :" + methodName);
            }
        }
        if (PoiUtil.isLegalGetMethod(method)) {
            return method;
        }
        else {
            throw new Exception("IlleaglGetMethod:" + method.getName());
        }
    }
}
